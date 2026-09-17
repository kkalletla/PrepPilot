package com.preppilot.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.preppilot.common.ApiException;
import com.preppilot.design.DesignQuestionRepository;
import com.preppilot.design.DesignService;
import com.preppilot.dsa.DsaService;
import com.preppilot.dsa.ProblemRepository;
import com.preppilot.subscription.*;
import com.preppilot.user.User;
import com.preppilot.user.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UsageGateTest {

    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired UsageCounterRepository counters;
    @Autowired ProblemRepository problems;
    @Autowired DesignQuestionRepository questions;
    @Autowired DsaService dsa;
    @Autowired DesignService design;
    @Autowired UsageGate gate;

    Long userId;

    @BeforeEach
    void user() {
        userId = users.save(new User("gate@test.dev", "hash")).getId();
        subscriptions.save(new Subscription(userId));
    }

    @Test
    void freeTierAllowsThreeNewDsaProblemsPerDay() {
        var ids = problems.findAll().stream().map(p -> p.getId()).toList();
        dsa.recordAttempt(userId, ids.get(0));
        dsa.recordAttempt(userId, ids.get(1));
        dsa.recordAttempt(userId, ids.get(2));
        dsa.recordAttempt(userId, ids.get(2));                       // same problem again: not a new session

        assertThatThrownBy(() -> dsa.requestHint(userId, ids.get(3), ""))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED))
                .hasMessageContaining("Upgrade");
        assertThat(gate.dsaUsedToday(userId)).isEqualTo(3);
    }

    @Test
    void freeTierAllowsOneDesignSessionPerRollingWeek() {
        Long q = questions.findAll().get(0).getId();
        design.startSession(userId, q);
        assertThatThrownBy(() -> design.startSession(userId, q)).isInstanceOf(ApiException.class);

        // A session 6 days ago still counts; 7 days ago does not.
        counters.deleteAll();
        UsageCounter old = new UsageCounter(userId, LocalDate.now().minusDays(7));
        old.incrementDesign();
        counters.save(old);
        assertThat(gate.designUsedThisWeek(userId)).isZero();
        design.startSession(userId, q);
        assertThat(gate.designUsedThisWeek(userId)).isEqualTo(1);
    }

    @Test
    void activePaidSubscriptionIsUnlimited() {
        Subscription s = subscriptions.findByUserId(userId).orElseThrow();
        s.setTier(SubscriptionTier.PAID);
        s.setStatus(SubscriptionStatus.ACTIVE);
        subscriptions.save(s);

        Long q = questions.findAll().get(0).getId();
        for (int i = 0; i < 3; i++) design.startSession(userId, q);
        for (var p : problems.findAll()) dsa.recordAttempt(userId, p.getId());
        assertThat(gate.isUnlimited(userId)).isTrue();
    }

    @Test
    void pastDuePaidSubscriptionIsGatedLikeFree() {
        Subscription s = subscriptions.findByUserId(userId).orElseThrow();
        s.setTier(SubscriptionTier.PAID);
        s.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptions.save(s);
        assertThat(gate.isUnlimited(userId)).isFalse();
    }
}
