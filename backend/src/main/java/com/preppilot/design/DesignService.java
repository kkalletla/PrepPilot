package com.preppilot.design;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppilot.coaching.CoachingEngine;
import com.preppilot.coaching.DesignAnswerRequest;
import com.preppilot.coaching.DesignFeedback;
import com.preppilot.coaching.RubricDimension;
import com.preppilot.coaching.TranscriptTurn;
import com.preppilot.coaching.TranscriptTurn.Role;
import com.preppilot.common.ApiException;
import com.preppilot.design.DesignDtos.*;
import com.preppilot.billing.UsageGate;
import com.preppilot.subscription.UsageService;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * System Design Mock Interview: intro -> requirements -> components -> data model -> scaling -> rubric.
 * Every stage answer is graded through {@link CoachingEngine}; per-stage feedback is kept in the transcript
 * (plain data) and aggregated into the final rubric when the last stage completes.
 */
@Service
public class DesignService {

    private static final TypeReference<List<TranscriptTurn>> TURNS = new TypeReference<>() {};
    private static final TypeReference<List<DesignFeedback>> FEEDBACKS = new TypeReference<>() {};

    private final DesignQuestionRepository questions;
    private final DesignSessionRepository sessions;
    private final CoachingEngine coach;
    private final UsageService usage;
    private final UsageGate gate;
    private final ObjectMapper json;

    public DesignService(DesignQuestionRepository questions, DesignSessionRepository sessions, CoachingEngine coach,
                         UsageService usage, UsageGate gate, ObjectMapper json) {
        this.questions = questions;
        this.sessions = sessions;
        this.coach = coach;
        this.usage = usage;
        this.gate = gate;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<QuestionSummary> listQuestions(Long userId, SeniorityLevel seniority) {
        List<DesignQuestion> list = seniority == null
                ? questions.findAllByOrderBySeniorityAscIdAsc()
                : questions.findBySeniorityOrderByIdAsc(seniority);
        boolean unlimited = gate.isUnlimited(userId);
        return list.stream().map(q -> QuestionSummary.of(q, gate.isSeniorityLocked(unlimited, q.getSeniority()))).toList();
    }

    /** Flow step 1: the intro prompt sets the problem. */
    @Transactional
    public SessionView startSession(Long userId, Long questionId) {
        DesignQuestion q = questions.findById(questionId).orElseThrow(() -> ApiException.notFound("design question"));
        gate.assertSeniorityAccessible(userId, q.getSeniority());
        gate.assertCanStartDesign(userId);
        DesignSession s = new DesignSession(userId, q.getId());
        List<TranscriptTurn> turns = new ArrayList<>();
        turns.add(new TranscriptTurn(Role.COACH, DesignStage.REQUIREMENTS, q.getPrompt()));
        s.setTranscript(write(turns));
        usage.recordDesignSession(userId);
        return view(sessions.save(s), q);
    }

    /** Flow steps 2-4: grade the current stage's answer, challenge it, advance; produce the rubric after SCALING. */
    @Transactional
    public AnswerResult answer(Long userId, Long sessionId, String answer) {
        DesignSession s = session(userId, sessionId);
        if (!s.getStage().isAnswerable()) {
            throw new ApiException(HttpStatus.CONFLICT, "session already complete");
        }
        DesignQuestion q = questions.findById(s.getQuestionId()).orElseThrow();
        DesignStage stage = s.getStage();
        List<TranscriptTurn> turns = read(s.getTranscript(), TURNS);

        DesignFeedback fb = coach.gradeDesignAnswer(new DesignAnswerRequest(q.getSlug(), stage, answer, List.copyOf(turns)));

        turns.add(new TranscriptTurn(Role.CANDIDATE, stage, answer));
        turns.add(new TranscriptTurn(Role.COACH, stage, fb.narrative() + " " + fb.followUpPrompt()));

        List<DesignFeedback> graded = read(s.getRubricBreakdown() == null ? "[]" : s.getRubricBreakdown(), FEEDBACKS);
        graded.add(fb);

        DesignStage next = stage.next().orElseThrow();
        if (next == DesignStage.COMPLETE) {
            RubricScore rubric = aggregate(graded);
            turns.add(new TranscriptTurn(Role.COACH, DesignStage.COMPLETE, rubric.narrative()));
            s.setTranscript(write(turns));
            s.complete(write(graded), rubric.overall());
        } else {
            s.setTranscript(write(turns));
            s.setRubricBreakdownDraft(write(graded));
            s.setStage(next);
        }
        return new AnswerResult(fb, view(sessions.save(s), q));
    }

    @Transactional(readOnly = true)
    public SessionView getSession(Long userId, Long sessionId) {
        DesignSession s = session(userId, sessionId);
        return view(s, questions.findById(s.getQuestionId()).orElseThrow());
    }

    /** Free users see only the recent history window; paid users see everything. */
    @Transactional(readOnly = true)
    public List<SessionView> listSessions(Long userId) {
        java.time.Instant cutoff = gate.historyCutoff(gate.isUnlimited(userId));
        return sessions.findByUserIdOrderByStartedAtDesc(userId).stream()
                .filter(s -> cutoff == null || !s.getStartedAt().isBefore(cutoff))
                .map(s -> view(s, questions.findById(s.getQuestionId()).orElseThrow()))
                .toList();
    }

    // ------------------------------------------------------------ rubric

    /** Average each dimension over the stages that scored it; overall is the mean of the four dimensions x10. */
    static RubricScore aggregate(List<DesignFeedback> graded) {
        Map<RubricDimension, Integer> dims = new EnumMap<>(RubricDimension.class);
        for (RubricDimension d : RubricDimension.values()) {
            int[] acc = graded.stream()
                    .map(f -> f.dimensionScores().get(d))
                    .filter(v -> v != null)
                    .reduce(new int[2], (a, v) -> new int[]{a[0] + v, a[1] + 1}, (a, b) -> new int[]{a[0] + b[0], a[1] + b[1]});
            dims.put(d, acc[1] == 0 ? 0 : (int) Math.round((double) acc[0] / acc[1]));
        }
        int overall = (int) Math.round(dims.values().stream().mapToInt(Integer::intValue).average().orElse(0) * 10);

        RubricDimension weakest = dims.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow().getKey();
        RubricDimension strongest = dims.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        List<String> allGaps = graded.stream().flatMap(f -> f.gaps().stream()).toList();
        String narrative = "Overall %d/100. Strongest area: %s. Weakest area: %s.%s".formatted(
                overall, pretty(strongest), pretty(weakest),
                allGaps.isEmpty() ? " You covered every key point — next time, go deeper on the trade-offs behind each choice."
                                  : " Revisit: " + String.join("; ", allGaps.subList(0, Math.min(4, allGaps.size()))) + ".");
        return new RubricScore(dims, overall, narrative);
    }

    private static String pretty(RubricDimension d) {
        return d.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    // ------------------------------------------------------------ helpers

    private DesignSession session(Long userId, Long id) {
        return sessions.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("design session"));
    }

    private SessionView view(DesignSession s, DesignQuestion q) {
        RubricScore rubric = null;
        if (s.getStage() == DesignStage.COMPLETE && s.getRubricBreakdown() != null) {
            rubric = aggregate(read(s.getRubricBreakdown(), FEEDBACKS));
        }
        return new SessionView(s.getId(), q.getId(), q.getTitle(), s.getStage(), read(s.getTranscript(), TURNS), rubric,
                s.getStartedAt(), s.getCompletedAt());
    }

    private <T> T read(String raw, TypeReference<T> type) {
        try {
            return json.readValue(raw, type);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
