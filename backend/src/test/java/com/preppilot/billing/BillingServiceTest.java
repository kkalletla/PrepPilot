package com.preppilot.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.preppilot.common.ApiException;
import com.preppilot.subscription.*;
import com.preppilot.user.User;
import com.preppilot.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BillingServiceTest {

    static final String SECRET = "whsec_test_secret_for_unit_tests";

    @Autowired BillingService billing;
    @Autowired FakeBillingGateway gateway;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired UserRepository users;

    Long userId;

    @BeforeEach
    void user() {
        userId = users.save(new User("bill@test.dev", "hash")).getId();
        subscriptions.save(new Subscription(userId));
    }

    @Test
    void checkoutUsesTheGatewayWithUserReference() {
        String url = billing.checkoutUrl(userId);
        assertThat(url).contains("session-for-" + userId);
        assertThat(gateway.lastEmail).isEqualTo("bill@test.dev");
        assertThat(gateway.lastCustomerId).isNull();
    }

    @Test
    void portalRequiresAnExistingStripeCustomer() {
        assertThatThrownBy(() -> billing.portalUrl(userId)).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsBadSignatureAndMissingHeader() {
        String payload = "{\"type\":\"invoice.paid\"}";
        assertThatThrownBy(() -> billing.handleWebhook(payload, "t=1,v1=deadbeef"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> billing.handleWebhook(payload, null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> billing.handleWebhook(payload, StripeSigner.sign(payload, "whsec_wrong")))
                .isInstanceOf(ApiException.class);
        // A valid signature that is too old is replay-rejected.
        assertThatThrownBy(() -> billing.handleWebhook(payload, StripeSigner.sign(payload, SECRET, 1_000_000)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void checkoutCompletedUpgradesToPaidAndStoresStripeIds() {
        String payload = """
                {"type":"checkout.session.completed","livemode":false,"data":{"object":{
                  "id":"cs_test_1","mode":"subscription","client_reference_id":"%d",
                  "customer":"cus_123","subscription":"sub_456"}}}
                """.formatted(userId);

        assertThat(billing.handleWebhook(payload, StripeSigner.sign(payload, SECRET))).isEqualTo("checkout.session.completed");

        Subscription s = subscriptions.findByUserId(userId).orElseThrow();
        assertThat(s.getTier()).isEqualTo(SubscriptionTier.PAID);
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(s.getStripeCustomerId()).isEqualTo("cus_123");
        assertThat(s.getStripeSubscriptionId()).isEqualTo("sub_456");
        assertThat(s.getRenewedAt()).isNotNull();
        assertThat(billing.status(userId).unlimited()).isTrue();
        assertThat(billing.status(userId).dsaDailyLimit()).isNull();
        assertThat(billing.portalUrl(userId)).endsWith("/cus_123");
    }

    @Test
    void subscriptionLifecycleIsReflectedFromWebhooks() {
        upgrade();

        String pastDue = sub("past_due", 1_700_000_000L);
        billing.handleWebhook(pastDue, StripeSigner.sign(pastDue, SECRET));
        Subscription s = subscriptions.findByUserId(userId).orElseThrow();
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(s.getRenewedAt().getEpochSecond()).isEqualTo(1_700_000_000L);
        assertThat(billing.status(userId).unlimited()).isFalse();

        String renewed = """
                {"type":"invoice.paid","livemode":false,"data":{"object":{"id":"in_1","customer":"cus_123"}}}
                """;
        billing.handleWebhook(renewed, StripeSigner.sign(renewed, SECRET));
        assertThat(subscriptions.findByUserId(userId).orElseThrow().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        String deleted = """
                {"type":"customer.subscription.deleted","livemode":false,"data":{"object":{"id":"sub_456","customer":"cus_123","status":"canceled"}}}
                """;
        billing.handleWebhook(deleted, StripeSigner.sign(deleted, SECRET));
        s = subscriptions.findByUserId(userId).orElseThrow();
        assertThat(s.getTier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(billing.status(userId).dsaDailyLimit()).isEqualTo(3);
    }

    @Test
    void liveModeEventsAreIgnoredWhileLiveIsNotAllowed() {
        String payload = """
                {"type":"checkout.session.completed","livemode":true,"data":{"object":{
                  "client_reference_id":"%d","customer":"cus_live","subscription":"sub_live"}}}
                """.formatted(userId);
        billing.handleWebhook(payload, StripeSigner.sign(payload, SECRET));
        assertThat(subscriptions.findByUserId(userId).orElseThrow().getTier()).isEqualTo(SubscriptionTier.FREE);
    }

    @Test
    void unknownCustomersAndEventTypesAreIgnoredSafely() {
        String unknown = sub("active", 0).replace("cus_123", "cus_nobody");
        assertThat(billing.handleWebhook(unknown, StripeSigner.sign(unknown, SECRET))).isEqualTo("customer.subscription.updated");
        String other = "{\"type\":\"payment_intent.created\",\"data\":{\"object\":{}}}";
        assertThat(billing.handleWebhook(other, StripeSigner.sign(other, SECRET))).isEqualTo("payment_intent.created");
    }

    private void upgrade() {
        Subscription s = subscriptions.findByUserId(userId).orElseThrow();
        s.setTier(SubscriptionTier.PAID);
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setStripeCustomerId("cus_123");
        s.setStripeSubscriptionId("sub_456");
        subscriptions.save(s);
    }

    private static String sub(String status, long periodStart) {
        return """
                {"type":"customer.subscription.updated","livemode":false,"data":{"object":{
                  "id":"sub_456","customer":{"id":"cus_123"},"status":"%s","current_period_start":%d}}}
                """.formatted(status, periodStart);
    }
}
