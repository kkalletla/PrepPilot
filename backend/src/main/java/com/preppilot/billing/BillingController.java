package com.preppilot.billing;

import com.preppilot.auth.AuthenticatedUser;
import com.preppilot.billing.BillingDtos.BillingStatus;
import com.preppilot.billing.BillingDtos.RedirectResponse;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billing;

    public BillingController(BillingService billing) {
        this.billing = billing;
    }

    @GetMapping("/status")
    public BillingStatus status(@AuthenticationPrincipal AuthenticatedUser user) {
        return billing.status(user.id());
    }

    @PostMapping("/checkout")
    public RedirectResponse checkout(@AuthenticationPrincipal AuthenticatedUser user) {
        return new RedirectResponse(billing.checkoutUrl(user.id()));
    }

    @PostMapping("/portal")
    public RedirectResponse portal(@AuthenticationPrincipal AuthenticatedUser user) {
        return new RedirectResponse(billing.portalUrl(user.id()));
    }

    /** Public endpoint; authenticity comes from the Stripe-Signature header, not a JWT. */
    @PostMapping("/webhook")
    public Map<String, String> webhook(@RequestBody String payload,
                                       @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        return Map.of("received", billing.handleWebhook(payload, signature));
    }
}
