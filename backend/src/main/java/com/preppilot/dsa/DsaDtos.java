package com.preppilot.dsa;

import com.preppilot.coaching.HintResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DsaDtos {
    private DsaDtos() {}

    public record ProblemSummary(Long id, String slug, String title, ProblemCategory category, DifficultyTier difficulty,
                                 ProgressStatus status, int hintsUsed, boolean locked) {
        static ProblemSummary of(Problem p, ProblemProgress progress, boolean locked) {
            return new ProblemSummary(p.getId(), p.getSlug(), p.getTitle(), p.getCategory(), p.getDifficulty(),
                    progress == null ? null : progress.getStatus(), progress == null ? 0 : progress.getHintsUsed(), locked);
        }
    }

    /** {@code locked} = paid-only tier for this user: the statement is visible, attempts and hints answer 402. */
    public record ProblemDetail(Long id, String slug, String title, ProblemCategory category, DifficultyTier difficulty,
                                String statement, ProgressView progress, boolean locked) {}

    public record ProgressView(Long problemId, DifficultyTier difficulty, int attempts, int hintsUsed,
                               ProgressStatus status, Instant lastAttemptAt) {
        static ProgressView of(ProblemProgress p) {
            return new ProgressView(p.getProblemId(), p.getDifficulty(), p.getAttempts(), p.getHintsUsed(),
                    p.getStatus(), p.getLastAttemptAt());
        }
    }

    public record AttemptRequest(String notes) {}

    public record HintRequestBody(String attempt) {}

    public record HintView(HintResponse hint, ProgressView progress) {}

    public record SolveResult(ProgressView progress, DifficultyTier recommendedTier, boolean escalated, int streakDays) {}

    /** {@code historyDays} is null for paid users (full history) and the free window otherwise. */
    public record DashboardView(int streakDays, int solvedCount, int inProgressCount,
                                Map<ProblemCategory, DifficultyTier> recommendedTiers, List<ProgressView> recent,
                                Integer historyDays) {}
}
