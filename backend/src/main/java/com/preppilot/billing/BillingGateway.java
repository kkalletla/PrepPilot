package com.preppilot.billing;

/** The only place that talks to Stripe's API. Tests swap in a fake; webhooks are handled separately. */
public interface BillingGateway {

    /** Creates a Checkout Session for the subscription price and returns the hosted URL to redirect to. */
    String createCheckoutUrl(Long userId, String email, String existingCustomerId);

    /** Creates a Customer Portal session and returns its URL. */
    String createPortalUrl(String customerId);
}
