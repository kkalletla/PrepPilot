package com.preppilot.subscription;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Per-user, per-day usage. Drives free-tier gating (wired tomorrow). */
@Entity
@Table(name = "usage_counters")
public class UsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "dsa_sessions_used", nullable = false)
    private int dsaSessionsUsed;

    @Column(name = "design_sessions_used", nullable = false)
    private int designSessionsUsed;

    protected UsageCounter() {}

    public UsageCounter(Long userId, LocalDate usageDate) {
        this.userId = userId;
        this.usageDate = usageDate;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getUsageDate() { return usageDate; }
    public int getDsaSessionsUsed() { return dsaSessionsUsed; }
    public int getDesignSessionsUsed() { return designSessionsUsed; }

    public void incrementDsa() { dsaSessionsUsed++; }
    public void incrementDesign() { designSessionsUsed++; }
}
