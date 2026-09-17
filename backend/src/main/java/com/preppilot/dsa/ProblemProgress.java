package com.preppilot.dsa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "problem_progress")
public class ProblemProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DifficultyTier difficulty;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "hints_used", nullable = false)
    private int hintsUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProgressStatus status = ProgressStatus.IN_PROGRESS;

    @Column(name = "last_attempt_at", nullable = false)
    private Instant lastAttemptAt;

    protected ProblemProgress() {}

    public ProblemProgress(Long userId, Long problemId, DifficultyTier difficulty) {
        this.userId = userId;
        this.problemId = problemId;
        this.difficulty = difficulty;
        this.lastAttemptAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProblemId() { return problemId; }
    public DifficultyTier getDifficulty() { return difficulty; }
    public int getAttempts() { return attempts; }
    public int getHintsUsed() { return hintsUsed; }
    public ProgressStatus getStatus() { return status; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }

    public void recordAttempt() {
        attempts++;
        lastAttemptAt = Instant.now();
    }

    public void recordHint() {
        hintsUsed++;
        lastAttemptAt = Instant.now();
    }

    public void markSolved() {
        status = ProgressStatus.SOLVED;
        lastAttemptAt = Instant.now();
    }

    /** Test/seed helper: set the attempt timestamp explicitly. */
    public void setLastAttemptAt(Instant at) { this.lastAttemptAt = at; }
}
