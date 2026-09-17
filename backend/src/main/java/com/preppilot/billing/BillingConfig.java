package com.preppilot.billing;

import com.preppilot.common.ApiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
@EnableConfigurationProperties({StripeProperties.class, LimitsProperties.class})
public class BillingConfig {

    /** Real Stripe when a secret key is configured; otherwise a gateway that explains billing is off. */
    @Bean
    @ConditionalOnMissingBean(BillingGateway.class)
    BillingGateway billingGateway(StripeProperties props) {
        if (props.configured()) return new StripeBillingGateway(props);
        return new BillingGateway() {
            @Override public String createCheckoutUrl(Long userId, String email, String existingCustomerId) { throw off(); }
            @Override public String createPortalUrl(String customerId) { throw off(); }
            private ApiException off() {
                return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "billing is not configured (set STRIPE_SECRET_KEY)");
            }
        };
    }
}
