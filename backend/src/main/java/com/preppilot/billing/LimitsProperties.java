package com.preppilot.billing;

import com.preppilot.design.SeniorityLevel;
import com.preppilot.dsa.DifficultyTier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Free-tier allowances. Paid + active users are unlimited.
 *
 * @param freeDsaPerDay       new DSA problems a free user may start per calendar day
 * @param freeDesignPerWeek   design sessions a free user may start per rolling 7 days
 * @param freeMaxTier         highest DSA tier open to free users (higher tiers are locked)
 * @param freeMaxSeniority    highest design-question seniority open to free users
 * @param freeHistoryDays     how far back a free user's history (progress, sessions) is shown
 */
@ConfigurationProperties(prefix = "preppilot.limits")
public record LimitsProperties(
        @DefaultValue("3") int freeDsaPerDay,
        @DefaultValue("1") int freeDesignPerWeek,
        @DefaultValue("MEDIUM") DifficultyTier freeMaxTier,
        @DefaultValue("SENIOR") SeniorityLevel freeMaxSeniority,
        @DefaultValue("7") int freeHistoryDays) {}
