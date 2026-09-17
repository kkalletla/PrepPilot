package com.preppilot.design;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.preppilot.coaching.DesignFeedback;
import com.preppilot.coaching.RubricDimension;
import com.preppilot.coaching.TranscriptTurn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DesignDtos {
    private DesignDtos() {}

    /** {@code locked} = paid-only seniority level for this user; starting a session answers 402. */
    public record QuestionSummary(Long id, String slug, String title, String category, SeniorityLevel seniority, boolean locked) {
        static QuestionSummary of(DesignQuestion q, boolean locked) {
            return new QuestionSummary(q.getId(), q.getSlug(), q.getTitle(), q.getCategory(), q.getSeniority(), locked);
        }
    }

    public record StartSessionRequest(@NotNull Long questionId) {}

    public record AnswerRequest(@NotBlank String answer) {}

    /** Final rubric: 0-10 per dimension, overall 0-100, plus narrative feedback. */
    public record RubricScore(Map<RubricDimension, Integer> dimensions, int overall, String narrative) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionView(Long id, Long questionId, String questionTitle, DesignStage stage,
                              List<TranscriptTurn> transcript, RubricScore rubric,
                              Instant startedAt, Instant completedAt) {}

    /** Returned after each answer: this stage's feedback, and the session as it now stands. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AnswerResult(DesignFeedback feedback, SessionView session) {}
}
