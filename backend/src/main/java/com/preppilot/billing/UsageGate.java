package com.preppilot.billing;

import com.preppilot.common.ApiException;
import com.preppilot.design.SeniorityLevel;
import com.preppilot.dsa.DifficultyTier;
import com.preppilot.subscription.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Usage-gating middleware: checks tier + daily/weekly counters before a session is served.
 * Free tier: N DSA problems per day and M design sessions per rolling 7 days. Paid+active: unlimited.
 * Limits hit -> 402 Payment Required with an upgrade prompt.
 */
@Service
public class UsageGate {

    private final SubscriptionRepository subscriptions;
    private final UsageCounterRepository counters;
    private final LimitsProperties limits;
    private final Clock clock;

    public UsageGate(SubscriptionRepository subscriptions, UsageCounterRepository counters, LimitsProperties limits, Clock clock) {
        this.subscriptions = subscriptions;
        this.counters = counters;
        this.limits = limits;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean isUnlimited(Long userId) {
        return subscriptions.findByUserId(userId)
                .map(s -> s.getTier() == SubscriptionTier.PAID && s.getStatus() == SubscriptionStatus.ACTIVE)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void assertCanStartDsa(Long userId) {
        if (isUnlimited(userId)) return;
        if (dsaUsedToday(userId) >= limits.freeDsaPerDay()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "Free tier allows " + limits.freeDsaPerDay() + " DSA problems per day. Upgrade for unlimited practice.");
        }
    }

    @Transactional(readOnly = true)
    public void assertCanStartDesign(Long userId) {
        if (isUnlimited(userId)) return;
        if (designUsedThisWeek(userId) >= limits.freeDesignPerWeek()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "Free tier allows " + limits.freeDesignPerWeek() + " system design session per week. Upgrade for unlimited sessions.");
        }
    }

    // ---------------------------------------------------------------- paid-only content

    /** True when this tier is above what the free plan includes. */
    public boolean isTierLocked(boolean unlimited, DifficultyTier tier) {
        return !unlimited && tier.ordinal() > limits.freeMaxTier().ordinal();
    }

    public boolean isSeniorityLocked(boolean unlimited, SeniorityLevel seniority) {
        return !unlimited && seniority.ordinal() > limits.freeMaxSeniority().ordinal();
    }

    @Transactional(readOnly = true)
    public void assertTierAccessible(Long userId, DifficultyTier tier) {
        if (isTierLocked(isUnlimited(userId), tier)) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    tier.name().replace('_', ' ') + " problems are part of Pro. Upgrade to unlock every tier.");
        }
    }

    @Transactional(readOnly = true)
    public void assertSeniorityAccessible(Long userId, SeniorityLevel seniority) {
        if (isSeniorityLocked(isUnlimited(userId), seniority)) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    seniority.name() + "-level questions are part of Pro. Upgrade to unlock the full question bank.");
        }
    }

    /** Oldest instant a free user's history goes back to; null (no cutoff) for paid users. */
    public Instant historyCutoff(boolean unlimited) {
        return unlimited ? null : Instant.now(clock).minus(limits.freeHistoryDays(), ChronoUnit.DAYS);
    }

    @Transactional(readOnly = true)
    public int dsaUsedToday(Long userId) {
        return counters.findByUserIdAndUsageDate(userId, LocalDate.now(clock)).map(UsageCounter::getDsaSessionsUsed).orElse(0);
    }

    @Transactional(readOnly = true)
    public int designUsedThisWeek(Long userId) {
        LocalDate today = LocalDate.now(clock);
        return counters.findByUserIdAndUsageDateBetween(userId, today.minusDays(6), today).stream()
                .mapToInt(UsageCounter::getDesignSessionsUsed).sum();
    }

    public LimitsProperties limits() { return limits; }
}
