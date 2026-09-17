package com.preppilot.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(String secretKey, String webhookSecret, String priceId, String successUrl,
                               String cancelUrl, String portalReturnUrl, boolean allowLive) {
    public boolean configured() {
        return secretKey != null && !secretKey.isBlank();
    }
}
