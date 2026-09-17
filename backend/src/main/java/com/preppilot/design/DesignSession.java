package com.preppilot.design;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "design_sessions")
public class DesignSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DesignStage stage = DesignStage.REQUIREMENTS;

    /** JSON array of turns; serialised by the service layer, provider-agnostic. */
    @Column(nullable = false, length = 100000)
    private String transcript = "[]";

    @Column(name = "rubric_breakdown", length = 4000)
    private String rubricBreakdown;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected DesignSession() {}

    public DesignSession(Long userId, Long questionId) {
        this.userId = userId;
        this.questionId = questionId;
        this.startedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getQuestionId() { return questionId; }
    public DesignStage getStage() { return stage; }
    public String getTranscript() { return transcript; }
    public String getRubricBreakdown() { return rubricBreakdown; }
    public Integer getOverallScore() { return overallScore; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void setStage(DesignStage stage) { this.stage = stage; }
    public void setTranscript(String transcript) { this.transcript = transcript; }

    /** Per-stage feedback accumulated while the session is still in progress. */
    public void setRubricBreakdownDraft(String rubricBreakdown) { this.rubricBreakdown = rubricBreakdown; }

    public void complete(String rubricBreakdown, int overallScore) {
        this.stage = DesignStage.COMPLETE;
        this.rubricBreakdown = rubricBreakdown;
        this.overallScore = overallScore;
        this.completedAt = Instant.now();
    }
}
