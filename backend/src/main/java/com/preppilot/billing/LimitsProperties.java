package com.preppilot.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Free-tier allowances. Paid users are unlimited. */
@ConfigurationProperties(prefix = "preppilot.limits")
public record LimitsProperties(int freeDsaPerDay, int freeDesignPerWeek) {}
