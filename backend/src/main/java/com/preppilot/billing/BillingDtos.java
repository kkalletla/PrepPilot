package com.preppilot.billing;

import com.preppilot.subscription.SubscriptionStatus;
import com.preppilot.subscription.SubscriptionTier;

public final class BillingDtos {
    private BillingDtos() {}

    public record RedirectResponse(String url) {}

    /** Current tier plus today's/this week's usage against the free-tier limits (limits are null when unlimited). */
    public record BillingStatus(SubscriptionTier tier, SubscriptionStatus status, boolean unlimited,
                                int dsaUsedToday, Integer dsaDailyLimit,
                                int designUsedThisWeek, Integer designWeeklyLimit,
                                boolean hasStripeCustomer, boolean billingConfigured) {}
}
