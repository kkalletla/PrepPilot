package com.preppilot.billing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Test double: records calls and returns predictable URLs so tests never reach Stripe. */
public class FakeBillingGateway implements BillingGateway {

    public Long lastUserId;
    public String lastEmail;
    public String lastCustomerId;

    @Override
    public String createCheckoutUrl(Long userId, String email, String existingCustomerId) {
        lastUserId = userId; lastEmail = email; lastCustomerId = existingCustomerId;
        return "https://checkout.stripe.test/session-for-" + userId;
    }

    @Override
    public String createPortalUrl(String customerId) {
        lastCustomerId = customerId;
        return "https://portal.stripe.test/" + customerId;
    }

    @Configuration
    public static class Config {
        @Bean @Primary
        FakeBillingGateway fakeBillingGateway() { return new FakeBillingGateway(); }
    }
}
