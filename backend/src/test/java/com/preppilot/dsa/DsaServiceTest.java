package com.preppilot.dsa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.preppilot.coaching.TemplateCoachingEngine;
import com.preppilot.common.ApiException;
import com.preppilot.dsa.DsaDtos.HintView;
import com.preppilot.dsa.DsaDtos.SolveResult;
import com.preppilot.subscription.UsageService;
import com.preppilot.user.User;
import com.preppilot.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DsaServiceTest {

    @Autowired ProblemRepository problems;
    @Autowired ProblemProgressRepository progressRepo;
    @Autowired UserRepository users;
    @Autowired UsageService usage;
    @Autowired TemplateCoachingEngine templates;
    @Autowired DsaService dsa;

    Long userId;

    @BeforeEach
    void user() {
        userId = users.save(new User("dsa@test.dev", "hash")).getId();
    }

    @Test
    void starterBankIsSeededAndEveryProblemHasHints() {
        List<Problem> all = problems.findAll();
        assertThat(all).hasSizeBetween(10, 15);
        assertThat(all).extracting(Problem::getCategory).contains(ProblemCategory.ARRAYS, ProblemCategory.LINKED_LISTS);
        assertThat(all).extracting(Problem::getDifficulty).contains(DifficultyTier.values());
        assertThat(all).extracting(Problem::getSlug).allMatch(templates.problemSlugs()::contains);
    }

    @Test
    void hintsDeepenEachCallAndAreTrackedOnProgress() {
        Long id = slug("two-sum");
        HintView h1 = dsa.requestHint(userId, id, "");
        HintView h2 = dsa.requestHint(userId, id, "tried nested loops");
        HintView h3 = dsa.requestHint(userId, id, "");
        HintView h4 = dsa.requestHint(userId, id, "");

        assertThat(h1.hint().depth()).isEqualTo(1);
        assertThat(h2.hint().depth()).isEqualTo(2);
        assertThat(h3.hint().depth()).isEqualTo(3);
        assertThat(h3.hint().exhausted()).isTrue();
        assertThat(h4.hint().hint()).isEqualTo(h3.hint().hint());
        assertThat(h4.progress().hintsUsed()).isEqualTo(4);
        assertThat(h4.progress().status()).isEqualTo(ProgressStatus.IN_PROGRESS);
    }

    @Test
    void solvedProblemRefusesFurtherHints() {
        Long id = slug("two-sum");
        dsa.markSolved(userId, id);
        assertThatThrownBy(() -> dsa.requestHint(userId, id, "")).isInstanceOf(ApiException.class);
    }

    @Test
    void firstTouchOfAProblemCountsOneDsaSession() {
        Long id = slug("contains-duplicate");
        dsa.recordAttempt(userId, id);
        dsa.requestHint(userId, id, "");
        dsa.recordAttempt(userId, id);
        assertThat(usage.today(userId).getDsaSessionsUsed()).isEqualTo(1);

        dsa.recordAttempt(userId, slug("two-sum"));
        assertThat(usage.today(userId).getDsaSessionsUsed()).isEqualTo(2);
    }

    @Test
    void threeCleanEasySolvesEscalateToMedium() {
        assertThat(dsa.recommendedTier(userId, ProblemCategory.ARRAYS)).isEqualTo(DifficultyTier.EASY);

        SolveResult r1 = dsa.markSolved(userId, slug("two-sum"));
        assertThat(r1.escalated()).isFalse();
        assertThat(r1.recommendedTier()).isEqualTo(DifficultyTier.EASY);

        dsa.requestHint(userId, slug("contains-duplicate"), "");             // one hint is still "minimal"
        dsa.markSolved(userId, slug("contains-duplicate"));
        SolveResult r3 = dsa.markSolved(userId, slug("best-time-to-buy-and-sell-stock"));

        assertThat(r3.escalated()).isTrue();
        assertThat(r3.recommendedTier()).isEqualTo(DifficultyTier.MEDIUM);
        assertThat(dsa.recommendedTier(userId, ProblemCategory.LINKED_LISTS)).isEqualTo(DifficultyTier.EASY);   // per category
    }

    @Test
    void heavyHintUsageBlocksEscalation() {
        Long dup = slug("contains-duplicate");
        dsa.requestHint(userId, dup, "");
        dsa.requestHint(userId, dup, "");
        dsa.markSolved(userId, dup);
        dsa.markSolved(userId, slug("two-sum"));
        SolveResult r = dsa.markSolved(userId, slug("best-time-to-buy-and-sell-stock"));

        assertThat(r.escalated()).isFalse();
        assertThat(r.recommendedTier()).isEqualTo(DifficultyTier.EASY);
    }

    @Test
    void faangBarDoesNotEscalateFurther() {
        dsa.markSolved(userId, slug("sliding-window-maximum"));
        dsa.markSolved(userId, slug("sliding-window-maximum"));                 // re-solve is idempotent
        assertThat(dsa.recommendedTier(userId, ProblemCategory.ARRAYS)).isEqualTo(DifficultyTier.FAANG_BAR);
    }

    @Test
    void streakCountsConsecutiveDaysEndingTodayOrYesterday() {
        Clock fixed = Clock.fixed(Instant.parse("2026-09-17T15:00:00Z"), ZoneOffset.UTC);
        DsaService svc = new DsaService(problems, progressRepo, templates, usage, fixed);
        assertThat(svc.streakDays(userId)).isZero();

        solvedOn("two-sum", LocalDate.of(2026, 9, 17));
        solvedOn("contains-duplicate", LocalDate.of(2026, 9, 16));
        solvedOn("reverse-linked-list", LocalDate.of(2026, 9, 15));
        solvedOn("linked-list-cycle", LocalDate.of(2026, 9, 12));               // gap: not part of the streak
        assertThat(svc.streakDays(userId)).isEqualTo(3);

        // Streak survives if the last solve was yesterday, breaks if it was two days ago.
        DsaService tomorrow = new DsaService(problems, progressRepo, templates, usage,
                Clock.fixed(Instant.parse("2026-09-18T09:00:00Z"), ZoneOffset.UTC));
        assertThat(tomorrow.streakDays(userId)).isEqualTo(3);
        DsaService dayAfter = new DsaService(problems, progressRepo, templates, usage,
                Clock.fixed(Instant.parse("2026-09-19T09:00:00Z"), ZoneOffset.UTC));
        assertThat(dayAfter.streakDays(userId)).isZero();
    }

    @Test
    void dashboardAggregatesProgress() {
        dsa.markSolved(userId, slug("two-sum"));
        dsa.requestHint(userId, slug("reverse-linked-list"), "");
        var dash = dsa.dashboard(userId);

        assertThat(dash.solvedCount()).isEqualTo(1);
        assertThat(dash.inProgressCount()).isEqualTo(1);
        assertThat(dash.streakDays()).isEqualTo(1);
        assertThat(dash.recommendedTiers()).containsEntry(ProblemCategory.ARRAYS, DifficultyTier.EASY);
        assertThat(dash.recent()).hasSize(2);
    }

    private Long slug(String slug) {
        return problems.findBySlug(slug).orElseThrow().getId();
    }

    private void solvedOn(String slug, LocalDate day) {
        ProblemProgress pp = new ProblemProgress(userId, slug(slug), DifficultyTier.EASY);
        pp.markSolved();
        pp.setLastAttemptAt(day.atTime(12, 0).toInstant(ZoneOffset.UTC));
        progressRepo.save(pp);
    }
}
