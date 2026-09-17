package com.preppilot.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BillingControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    String auth;
    long userId;

    @BeforeEach
    void register() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"billing-http@test.dev\",\"password\":\"hunter22!\"}"))
                .andReturn().getResponse().getContentAsString();
        auth = "Bearer " + json.readTree(body).get("token").asText();
        userId = json.readTree(body).get("userId").asLong();
    }

    @Test
    void statusShowsFreeTierLimits() throws Exception {
        mvc.perform(get("/api/billing/status").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("FREE"))
                .andExpect(jsonPath("$.unlimited").value(false))
                .andExpect(jsonPath("$.dsaDailyLimit").value(3))
                .andExpect(jsonPath("$.designWeeklyLimit").value(1))
                .andExpect(jsonPath("$.dsaUsedToday").value(0));
    }

    @Test
    void checkoutReturnsRedirectUrl() throws Exception {
        mvc.perform(post("/api/billing/checkout").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.test/session-for-" + userId));
    }

    @Test
    void webhookIsPublicButSignatureChecked() throws Exception {
        String payload = "{\"type\":\"invoice.paid\",\"data\":{\"object\":{}}}";
        mvc.perform(post("/api/billing/webhook").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/billing/webhook").contentType(MediaType.APPLICATION_JSON).content(payload)
                        .header("Stripe-Signature", StripeSigner.sign(payload, BillingServiceTest.SECRET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value("invoice.paid"));
    }

    @Test
    void hittingTheFreeLimitReturns402() throws Exception {
        String ids = mvc.perform(get("/api/dsa/problems").header("Authorization", auth)).andReturn().getResponse().getContentAsString();
        var list = json.readTree(ids);
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/dsa/problems/" + list.get(i).get("id").asLong() + "/attempts").header("Authorization", auth))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/dsa/problems/" + list.get(3).get("id").asLong() + "/attempts").header("Authorization", auth))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Upgrade")));
    }
}
