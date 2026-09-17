package com.preppilot.billing;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.billingportal.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.Mode;

public class StripeBillingGateway implements BillingGateway {

    private final StripeClient client;
    private final StripeProperties props;

    public StripeBillingGateway(StripeProperties props) {
        if (props.secretKey().startsWith("sk_live_") && !props.allowLive()) {
            throw new IllegalStateException(
                    "Refusing to start with a LIVE Stripe key. Use a test key (sk_test_…) or set stripe.allow-live=true after the final review.");
        }
        this.props = props;
        this.client = new StripeClient(props.secretKey());
    }

    @Override
    public String createCheckoutUrl(Long userId, String email, String existingCustomerId) {
        var params = com.stripe.param.checkout.SessionCreateParams.builder()
                .setMode(Mode.SUBSCRIPTION)
                .addLineItem(LineItem.builder().setPrice(props.priceId()).setQuantity(1L).build())
                .setSuccessUrl(props.successUrl())
                .setCancelUrl(props.cancelUrl())
                .setClientReferenceId(String.valueOf(userId))
                .putMetadata("userId", String.valueOf(userId));
        if (existingCustomerId != null) params.setCustomer(existingCustomerId);
        else params.setCustomerEmail(email);
        try {
            return client.checkout().sessions().create(params.build()).getUrl();
        } catch (StripeException e) {
            throw new BillingException("Stripe checkout failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String createPortalUrl(String customerId) {
        try {
            return client.billingPortal().sessions().create(SessionCreateParams.builder()
                    .setCustomer(customerId)
                    .setReturnUrl(props.portalReturnUrl())
                    .build()).getUrl();
        } catch (StripeException e) {
            throw new BillingException("Stripe portal failed: " + e.getMessage(), e);
        }
    }
}
