package com.preppilot.billing;

import com.stripe.net.Webhook;

/** Builds a Stripe-Signature header exactly as Stripe does (t=<ts>,v1=<hmac(ts.payload)>). */
final class StripeSigner {
    private StripeSigner() {}

    static String sign(String payload, String secret) {
        return sign(payload, secret, System.currentTimeMillis() / 1000);
    }

    static String sign(String payload, String secret, long timestamp) {
        try {
            String v1 = Webhook.Util.computeHmacSha256(secret, timestamp + "." + payload);
            return "t=" + timestamp + ",v1=" + v1;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
