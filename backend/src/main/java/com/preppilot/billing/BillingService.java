package com.preppilot.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppilot.billing.BillingDtos.BillingStatus;
import com.preppilot.common.ApiException;
import com.preppilot.subscription.*;
import com.preppilot.user.User;
import com.preppilot.user.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final long WEBHOOK_TOLERANCE_SECONDS = 300;

    private final SubscriptionRepository subscriptions;
    private final UserRepository users;
    private final BillingGateway gateway;
    private final UsageGate gate;
    private final StripeProperties stripe;
    private final ObjectMapper json;

    public BillingService(SubscriptionRepository subscriptions, UserRepository users, BillingGateway gateway,
                          UsageGate gate, StripeProperties stripe, ObjectMapper json) {
        this.subscriptions = subscriptions;
        this.users = users;
        this.gateway = gateway;
        this.gate = gate;
        this.stripe = stripe;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public BillingStatus status(Long userId) {
        Subscription s = subscription(userId);
        boolean unlimited = gate.isUnlimited(userId);
        return new BillingStatus(s.getTier(), s.getStatus(), unlimited,
                gate.dsaUsedToday(userId), unlimited ? null : gate.limits().freeDsaPerDay(),
                gate.designUsedThisWeek(userId), unlimited ? null : gate.limits().freeDesignPerWeek(),
                s.getStripeCustomerId() != null, stripe.configured());
    }

    @Transactional(readOnly = true)
    public String checkoutUrl(Long userId) {
        Subscription s = subscription(userId);
        if (gate.isUnlimited(userId)) throw new ApiException(HttpStatus.CONFLICT, "already subscribed");
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("user"));
        return gateway.createCheckoutUrl(userId, u.getEmail(), s.getStripeCustomerId());
    }

    @Transactional(readOnly = true)
    public String portalUrl(Long userId) {
        Subscription s = subscription(userId);
        if (s.getStripeCustomerId() == null) throw new ApiException(HttpStatus.CONFLICT, "no billing account yet");
        return gateway.createPortalUrl(s.getStripeCustomerId());
    }

    // ------------------------------------------------------------ webhooks

    /**
     * Verifies the Stripe signature and applies the event. The payload is read as plain JSON rather than via the
     * SDK's typed models so the handler is not coupled to a specific Stripe API version.
     * Returns the event type that was handled (or ignored) for logging/testing.
     */
    @Transactional
    public String handleWebhook(String payload, String signatureHeader) {
        if (stripe.webhookSecret() == null || stripe.webhookSecret().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "webhook secret not configured");
        }
        try {
            Webhook.Signature.verifyHeader(payload, signatureHeader == null ? "" : signatureHeader,
                    stripe.webhookSecret(), WEBHOOK_TOLERANCE_SECONDS);
        } catch (SignatureVerificationException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid webhook signature");
        }

        JsonNode event;
        try {
            event = json.readTree(payload);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "malformed webhook payload");
        }
        String type = event.path("type").asText("");
        JsonNode obj = event.path("data").path("object");
        boolean live = event.path("livemode").asBoolean(false);
        if (live && !stripe.allowLive()) {
            log.warn("Ignoring LIVE-mode Stripe event {} while stripe.allow-live=false", type);
            return type;
        }

        switch (type) {
            case "checkout.session.completed" -> onCheckoutCompleted(obj);
            case "customer.subscription.created", "customer.subscription.updated" -> onSubscriptionChanged(obj);
            case "customer.subscription.deleted" -> onSubscriptionDeleted(obj);
            case "invoice.paid" -> onInvoicePaid(obj);
            default -> log.debug("Ignoring Stripe event {}", type);
        }
        return type;
    }

    private void onCheckoutCompleted(JsonNode session) {
        String userIdText = session.path("client_reference_id").asText(null);
        if (userIdText == null) userIdText = session.path("metadata").path("userId").asText(null);
        if (userIdText == null) { log.warn("checkout.session.completed without user reference"); return; }
        Subscription s = subscription(Long.parseLong(userIdText));
        s.setStripeCustomerId(text(session, "customer"));
        s.setStripeSubscriptionId(text(session, "subscription"));
        s.setTier(SubscriptionTier.PAID);
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setRenewedAt(Instant.now());
        subscriptions.save(s);
    }

    private void onSubscriptionChanged(JsonNode sub) {
        byCustomer(sub).ifPresent(s -> {
            s.setStripeSubscriptionId(text(sub, "id"));
            String stripeStatus = sub.path("status").asText("");
            switch (stripeStatus) {
                case "active", "trialing" -> { s.setTier(SubscriptionTier.PAID); s.setStatus(SubscriptionStatus.ACTIVE); }
                case "past_due", "unpaid", "incomplete" -> { s.setTier(SubscriptionTier.PAID); s.setStatus(SubscriptionStatus.PAST_DUE); }
                case "canceled", "incomplete_expired" -> { s.setTier(SubscriptionTier.FREE); s.setStatus(SubscriptionStatus.CANCELED); }
                default -> log.warn("Unknown Stripe subscription status {}", stripeStatus);
            }
            long periodStart = sub.path("current_period_start").asLong(0);
            if (periodStart > 0) s.setRenewedAt(Instant.ofEpochSecond(periodStart));
            subscriptions.save(s);
        });
    }

    private void onSubscriptionDeleted(JsonNode sub) {
        byCustomer(sub).ifPresent(s -> {
            s.setTier(SubscriptionTier.FREE);
            s.setStatus(SubscriptionStatus.CANCELED);
            subscriptions.save(s);
        });
    }

    private void onInvoicePaid(JsonNode invoice) {
        byCustomer(invoice).ifPresent(s -> {
            s.setTier(SubscriptionTier.PAID);
            s.setStatus(SubscriptionStatus.ACTIVE);
            s.setRenewedAt(Instant.now());
            subscriptions.save(s);
        });
    }

    private Optional<Subscription> byCustomer(JsonNode obj) {
        String customer = text(obj, "customer");
        if (customer == null) return Optional.empty();
        Optional<Subscription> found = subscriptions.findByStripeCustomerId(customer);
        if (found.isEmpty()) log.warn("Stripe event for unknown customer {}", customer);
        return found;
    }

    /** Stripe sends related objects either as an id string or an expanded object with an id. */
    private static String text(JsonNode obj, String field) {
        JsonNode n = obj.path(field);
        if (n.isTextual()) return n.asText();
        if (n.isObject() && n.hasNonNull("id")) return n.get("id").asText();
        return null;
    }

    private Subscription subscription(Long userId) {
        return subscriptions.findByUserId(userId).orElseGet(() -> subscriptions.save(new Subscription(userId)));
    }
}
