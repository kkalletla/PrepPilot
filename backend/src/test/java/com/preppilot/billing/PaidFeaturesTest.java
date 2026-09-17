package com.preppilot.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.preppilot.common.ApiException;
import com.preppilot.design.*;
import com.preppilot.dsa.*;
import com.preppilot.subscription.*;
import com.preppilot.user.User;
import com.preppilot.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

/** Paid-only content: HARD/FAANG_BAR problems, STAFF design questions, and full history. */
@SpringBootTest
@Transactional
class PaidFeaturesTest {

    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired ProblemRepository problems;
    @Autowired ProblemProgressRepository progressRepo;
    @Autowired DesignQuestionRepository questions;
    @Autowired DesignSessionRepository sessions;
    @Autowired DsaService dsa;
    @Autowired DesignService design;

    Long userId;

    @BeforeEach
    void user() {
        userId = users.save(new User("paid@test.dev", "hash")).getId();
        subscriptions.save(new Subscription(userId));
    }

    @Test
    void hardAndFaangBarProblemsAreLockedForFreeUsers() {
        var list = dsa.listProblems(userId, null, null);
        assertThat(list).filteredOn(p -> p.difficulty() == DifficultyTier.EASY || p.difficulty() == DifficultyTier.MEDIUM)
                .allMatch(p -> !p.locked());
        assertThat(list).filteredOn(p -> p.difficulty() == DifficultyTier.HARD || p.difficulty() == DifficultyTier.FAANG_BAR)
                .isNotEmpty().allMatch(DsaDtos.ProblemSummary::locked);

        Long hard = problems.findBySlug("trapping-rain-water").orElseThrow().getId();
        assertThat(dsa.getProblem(userId, hard).locked()).isTrue();            // statement still viewable
        assertThatThrownBy(() -> dsa.requestHint(userId, hard, ""))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED))
                .hasMessageContaining("Pro");
        assertThatThrownBy(() -> dsa.recordAttempt(userId, hard)).isInstanceOf(ApiException.class);
        assertThat(progressRepo.findByUserIdAndProblemId(userId, hard)).isEmpty();   // nothing was started or counted
    }

    @Test
    void paidUsersSeeNothingLocked() {
        upgrade();
        assertThat(dsa.listProblems(userId, null, null)).noneMatch(DsaDtos.ProblemSummary::locked);
        assertThat(design.listQuestions(userId, null)).noneMatch(DesignDtos.QuestionSummary::locked);
        Long hard = problems.findBySlug("merge-k-sorted-lists").orElseThrow().getId();
        assertThat(dsa.requestHint(userId, hard, "").hint().depth()).isEqualTo(1);
    }

    @Test
    void staffQuestionIsLockedForFreeUsers() {
        var list = design.listQuestions(userId, null);
        assertThat(list).filteredOn(q -> q.seniority() == SeniorityLevel.STAFF).isNotEmpty().allMatch(DesignDtos.QuestionSummary::locked);
        assertThat(list).filteredOn(q -> q.seniority() != SeniorityLevel.STAFF).allMatch(q -> !q.locked());

        Long staff = questions.findBySlug("news-feed").orElseThrow().getId();
        assertThatThrownBy(() -> design.startSession(userId, staff))
                .isInstanceOf(ApiException.class).hasMessageContaining("STAFF");
        assertThat(sessions.findByUserIdOrderByStartedAtDesc(userId)).isEmpty();
    }

    @Test
    void freeHistoryIsLimitedToSevenDaysPaidIsFull() {
        Long q = questions.findBySlug("url-shortener").orElseThrow().getId();
        DesignSession old = new DesignSession(userId, q);
        old.setStartedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        sessions.save(old);
        DesignSession fresh = sessions.save(new DesignSession(userId, q));

        ProblemProgress oldP = new ProblemProgress(userId, problems.findBySlug("two-sum").orElseThrow().getId(), DifficultyTier.EASY);
        oldP.markSolved();
        oldP.setLastAttemptAt(Instant.now().minus(10, ChronoUnit.DAYS));
        progressRepo.save(oldP);

        assertThat(design.listSessions(userId)).extracting(DesignDtos.SessionView::id).containsExactly(fresh.getId());
        var dash = dsa.dashboard(userId);
        assertThat(dash.recent()).isEmpty();
        assertThat(dash.solvedCount()).isEqualTo(1);           // counts stay honest, only the list is windowed
        assertThat(dash.historyDays()).isEqualTo(7);

        upgrade();
        assertThat(design.listSessions(userId)).hasSize(2);
        assertThat(dsa.dashboard(userId).recent()).hasSize(1);
        assertThat(dsa.dashboard(userId).historyDays()).isNull();
    }

    private void upgrade() {
        Subscription s = subscriptions.findByUserId(userId).orElseThrow();
        s.setTier(SubscriptionTier.PAID);
        s.setStatus(SubscriptionStatus.ACTIVE);
        subscriptions.save(s);
    }
}
