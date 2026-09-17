package com.preppilot.dsa;

import com.preppilot.coaching.CoachingEngine;
import com.preppilot.coaching.HintRequest;
import com.preppilot.coaching.HintResponse;
import com.preppilot.common.ApiException;
import com.preppilot.dsa.DsaDtos.*;
import com.preppilot.billing.UsageGate;
import com.preppilot.subscription.UsageService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSA Socratic Coach: hint flow, progress tracking, streaks and difficulty escalation.
 * All coaching content comes from {@link CoachingEngine}; this class never knows which engine is active.
 */
@Service
public class DsaService {

    /** Consecutive low-hint solves at a tier before recommending the next tier. */
    static final int ESCALATION_STREAK = 3;
    /** Maximum hints a solve may have used to count toward escalation. */
    static final int MAX_HINTS_FOR_ESCALATION = 1;

    private final ProblemRepository problems;
    private final ProblemProgressRepository progressRepo;
    private final CoachingEngine coach;
    private final UsageService usage;
    private final UsageGate gate;
    private final Clock clock;

    public DsaService(ProblemRepository problems, ProblemProgressRepository progressRepo, CoachingEngine coach,
                      UsageService usage, UsageGate gate, Clock clock) {
        this.problems = problems;
        this.progressRepo = progressRepo;
        this.coach = coach;
        this.usage = usage;
        this.gate = gate;
        this.clock = clock;
    }

    // ------------------------------------------------------------ catalogue

    @Transactional(readOnly = true)
    public List<ProblemSummary> listProblems(Long userId, ProblemCategory category, DifficultyTier difficulty) {
        List<Problem> list;
        if (category != null && difficulty != null) list = problems.findByCategoryAndDifficultyOrderByIdAsc(category, difficulty);
        else if (category != null) list = problems.findByCategoryOrderByDifficultyAscIdAsc(category);
        else if (difficulty != null) list = problems.findByDifficultyOrderByIdAsc(difficulty);
        else list = problems.findAllByOrderByCategoryAscDifficultyAscIdAsc();

        Map<Long, ProblemProgress> byProblem = progressRepo.findByUserIdOrderByLastAttemptAtDesc(userId).stream()
                .collect(Collectors.toMap(ProblemProgress::getProblemId, p -> p));
        boolean unlimited = gate.isUnlimited(userId);
        return list.stream()
                .map(p -> ProblemSummary.of(p, byProblem.get(p.getId()), gate.isTierLocked(unlimited, p.getDifficulty())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProblemDetail getProblem(Long userId, Long problemId) {
        Problem p = problem(problemId);
        ProgressView pv = progressRepo.findByUserIdAndProblemId(userId, problemId).map(ProgressView::of).orElse(null);
        boolean locked = gate.isTierLocked(gate.isUnlimited(userId), p.getDifficulty());
        return new ProblemDetail(p.getId(), p.getSlug(), p.getTitle(), p.getCategory(), p.getDifficulty(), p.getStatement(), pv, locked);
    }

    // ------------------------------------------------------------ hint flow

    /** Step 1 of the hint flow: the user submits an attempt (code / notes). */
    @Transactional
    public ProgressView recordAttempt(Long userId, Long problemId) {
        Problem p = problem(problemId);
        gate.assertTierAccessible(userId, p.getDifficulty());
        ProblemProgress pp = progressFor(userId, p);
        pp.recordAttempt();
        return ProgressView.of(progressRepo.save(pp));
    }

    /** Step 2: graduated hint from the engine; hint depth is tracked per problem and feeds scoring. */
    @Transactional
    public HintView requestHint(Long userId, Long problemId, String attempt) {
        Problem p = problem(problemId);
        gate.assertTierAccessible(userId, p.getDifficulty());
        ProblemProgress pp = progressFor(userId, p);
        if (pp.getStatus() == ProgressStatus.SOLVED) {
            throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, "problem already solved");
        }
        HintResponse hint = coach.generateHint(new HintRequest(p.getSlug(), p.getDifficulty(), attempt, pp.getHintsUsed()));
        pp.recordHint();
        return new HintView(hint, ProgressView.of(progressRepo.save(pp)));
    }

    /** Step 3: mark solved; report whether the user's tier should escalate (step 4 of the spec). */
    @Transactional
    public SolveResult markSolved(Long userId, Long problemId) {
        Problem p = problem(problemId);
        ProblemProgress pp = progressFor(userId, p);
        pp.markSolved();
        progressRepo.save(pp);
        DifficultyTier recommended = recommendedTier(userId, p.getCategory());
        boolean escalated = recommended.ordinal() > p.getDifficulty().ordinal();
        return new SolveResult(ProgressView.of(pp), recommended, escalated, streakDays(userId));
    }

    // ------------------------------------------------------------ dashboard

    @Transactional(readOnly = true)
    public DashboardView dashboard(Long userId) {
        List<ProblemProgress> all = progressRepo.findByUserIdOrderByLastAttemptAtDesc(userId);
        int solved = (int) all.stream().filter(p -> p.getStatus() == ProgressStatus.SOLVED).count();
        Map<ProblemCategory, DifficultyTier> tiers = new EnumMap<>(ProblemCategory.class);
        for (ProblemCategory c : ProblemCategory.values()) tiers.put(c, recommendedTier(userId, c));
        boolean unlimited = gate.isUnlimited(userId);
        java.time.Instant cutoff = gate.historyCutoff(unlimited);
        List<ProgressView> recent = all.stream()
                .filter(p -> cutoff == null || !p.getLastAttemptAt().isBefore(cutoff))
                .limit(10).map(ProgressView::of).toList();
        return new DashboardView(streakDays(userId), solved, all.size() - solved, tiers, recent,
                unlimited ? null : gate.limits().freeHistoryDays());
    }

    /**
     * Auto-escalation: if the user's last {@value ESCALATION_STREAK} solves in this category were all at the same tier
     * and each used at most {@value MAX_HINTS_FOR_ESCALATION} hint, recommend the next tier. Otherwise recommend the tier
     * of the most recent solve (or EASY if nothing is solved yet).
     */
    @Transactional(readOnly = true)
    public DifficultyTier recommendedTier(Long userId, ProblemCategory category) {
        Map<Long, Problem> catalogue = problems.findAll().stream().collect(Collectors.toMap(Problem::getId, p -> p));
        List<ProblemProgress> solvedInCategory = progressRepo
                .findByUserIdAndStatusOrderByLastAttemptAtDesc(userId, ProgressStatus.SOLVED).stream()
                .filter(pp -> catalogue.containsKey(pp.getProblemId()) && catalogue.get(pp.getProblemId()).getCategory() == category)
                .toList();
        if (solvedInCategory.isEmpty()) return DifficultyTier.EASY;

        DifficultyTier current = solvedInCategory.get(0).getDifficulty();
        if (solvedInCategory.size() < ESCALATION_STREAK) return current;
        boolean streak = solvedInCategory.subList(0, ESCALATION_STREAK).stream()
                .allMatch(pp -> pp.getDifficulty() == current && pp.getHintsUsed() <= MAX_HINTS_FOR_ESCALATION);
        return streak ? current.next().orElse(current) : current;
    }

    /** Daily solve streak: consecutive calendar days (ending today or yesterday) with at least one solve. */
    @Transactional(readOnly = true)
    public int streakDays(Long userId) {
        Set<LocalDate> days = progressRepo.findByUserIdAndStatusOrderByLastAttemptAtDesc(userId, ProgressStatus.SOLVED).stream()
                .map(pp -> LocalDate.ofInstant(pp.getLastAttemptAt(), clock.getZone()))
                .collect(Collectors.toSet());
        LocalDate today = LocalDate.now(clock);
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    // ------------------------------------------------------------ helpers

    private Problem problem(Long id) {
        return problems.findById(id).orElseThrow(() -> ApiException.notFound("problem"));
    }

    /** Returns existing progress, or starts a new one: gated by tier/limits and counted toward today's usage. */
    private ProblemProgress progressFor(Long userId, Problem p) {
        return progressRepo.findByUserIdAndProblemId(userId, p.getId()).orElseGet(() -> {
            gate.assertCanStartDsa(userId);
            usage.recordDsaSession(userId);
            return progressRepo.save(new ProblemProgress(userId, p.getId(), p.getDifficulty()));
        });
    }
}
