package com.preppilot.design;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.preppilot.coaching.DesignFeedback;
import com.preppilot.coaching.RubricDimension;
import com.preppilot.coaching.TemplateCoachingEngine;
import com.preppilot.coaching.TranscriptTurn.Role;
import com.preppilot.common.ApiException;
import com.preppilot.design.DesignDtos.AnswerResult;
import com.preppilot.design.DesignDtos.RubricScore;
import com.preppilot.design.DesignDtos.SessionView;
import com.preppilot.subscription.UsageService;
import com.preppilot.user.User;
import com.preppilot.user.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DesignServiceTest {

    @Autowired DesignQuestionRepository questions;
    @Autowired DesignSessionRepository sessions;
    @Autowired UserRepository users;
    @Autowired UsageService usage;
    @Autowired TemplateCoachingEngine templates;
    @Autowired DesignService design;

    Long userId;

    @BeforeEach
    void user() {
        userId = users.save(new User("design@test.dev", "hash")).getId();
    }

    @Test
    void starterBankIsSeededWithTemplatesAndSeniorityTags() {
        List<DesignQuestion> all = questions.findAll();
        assertThat(all).hasSize(4);
        assertThat(all).extracting(DesignQuestion::getSlug).allMatch(templates.questionSlugs()::contains);
        assertThat(all).extracting(DesignQuestion::getSeniority).contains(SeniorityLevel.MID, SeniorityLevel.SENIOR, SeniorityLevel.STAFF);
        assertThat(design.listQuestions(userId, SeniorityLevel.STAFF)).extracting(q -> q.slug()).containsExactly("news-feed");
    }

    @Test
    void startOpensWithIntroAndCountsUsage() {
        SessionView s = design.startSession(userId, id("url-shortener"));

        assertThat(s.stage()).isEqualTo(DesignStage.REQUIREMENTS);
        assertThat(s.transcript()).hasSize(1);
        assertThat(s.transcript().get(0).role()).isEqualTo(Role.COACH);
        assertThat(s.transcript().get(0).content()).contains("URL shortening");
        assertThat(s.rubric()).isNull();
        assertThat(usage.today(userId).getDesignSessionsUsed()).isEqualTo(1);
    }

    @Test
    void fullStagedFlowEndsWithRubric() {
        SessionView s = design.startSession(userId, id("url-shortener"));

        AnswerResult r1 = design.answer(userId, s.id(), STRONG_REQUIREMENTS);
        assertThat(r1.feedback().stage()).isEqualTo(DesignStage.REQUIREMENTS);
        assertThat(r1.feedback().gaps()).isEmpty();
        assertThat(r1.session().stage()).isEqualTo(DesignStage.COMPONENTS);
        assertThat(r1.session().transcript()).hasSize(3);          // intro, candidate, coach follow-up
        assertThat(r1.session().rubric()).isNull();

        AnswerResult r2 = design.answer(userId, s.id(), "I'd put an API behind a load balancer, generate keys with base62 counters, cache hot redirects in Redis, and persist mappings in a database.");
        assertThat(r2.feedback().gaps()).isEmpty();
        assertThat(r2.session().stage()).isEqualTo(DesignStage.DATA_MODEL);

        AnswerResult r3 = design.answer(userId, s.id(), "Table mapping short code (primary key) to long url, plus created timestamp and owner. A key-value store like DynamoDB fits the point-lookup access pattern better than relational.");
        assertThat(r3.session().stage()).isEqualTo(DesignStage.SCALING);

        AnswerResult r4 = design.answer(userId, s.id(), "Shard by short code with consistent hashing, replicate for availability, LRU cache in front, and handle collisions by retrying key generation. Click analytics go through a Kafka queue asynchronously because the redirect path must stay fast.");
        assertThat(r4.session().stage()).isEqualTo(DesignStage.COMPLETE);
        assertThat(r4.session().completedAt()).isNotNull();

        RubricScore rubric = r4.session().rubric();
        assertThat(rubric).isNotNull();
        assertThat(rubric.dimensions()).containsKeys(RubricDimension.values());
        assertThat(rubric.dimensions().get(RubricDimension.SCALABILITY)).isGreaterThanOrEqualTo(8);
        assertThat(rubric.dimensions().get(RubricDimension.DATA_MODELING)).isGreaterThanOrEqualTo(8);
        assertThat(rubric.overall()).isBetween(60, 100);
        assertThat(rubric.narrative()).contains("Overall " + rubric.overall());
        assertThat(r4.session().transcript().get(r4.session().transcript().size() - 1).stage()).isEqualTo(DesignStage.COMPLETE);

        DesignSession stored = sessions.findById(s.id()).orElseThrow();
        assertThat(stored.getOverallScore()).isEqualTo(rubric.overall());
        assertThat(stored.getRubricBreakdown()).contains("\"stage\":\"SCALING\"");
    }

    @Test
    void weakAnswersAreChallengedAndScoreLow() {
        SessionView s = design.startSession(userId, id("rate-limiter"));
        AnswerResult r = design.answer(userId, s.id(), "just use redis");

        assertThat(r.feedback().stageScore()).isLessThanOrEqualTo(3);
        assertThat(r.feedback().gaps()).isNotEmpty();
        assertThat(r.feedback().followUpPrompt()).contains(r.feedback().gaps().get(0));

        for (int i = 0; i < 3; i++) design.answer(userId, s.id(), "redis");
        RubricScore rubric = design.getSession(userId, s.id()).rubric();
        assertThat(rubric.overall()).isLessThan(30);
        assertThat(rubric.narrative()).contains("Revisit");
    }

    @Test
    void completedSessionRejectsMoreAnswers() {
        SessionView s = design.startSession(userId, id("chat-application"));
        for (int i = 0; i < 4; i++) design.answer(userId, s.id(), "websockets and kafka");
        assertThatThrownBy(() -> design.answer(userId, s.id(), "more")).isInstanceOf(ApiException.class);
    }

    @Test
    void sessionsAreScopedToTheirOwner() {
        Long other = users.save(new User("other@test.dev", "hash")).getId();
        SessionView s = design.startSession(userId, id("chat-application"));
        assertThatThrownBy(() -> design.getSession(other, s.id())).isInstanceOf(ApiException.class);
        assertThat(design.listSessions(userId)).hasSize(1);
        assertThat(design.listSessions(other)).isEmpty();
    }

    @Test
    void aggregateAveragesOnlyStagesThatScoredEachDimension() {
        DesignFeedback a = fb(DesignStage.REQUIREMENTS, Map.of(RubricDimension.SCALABILITY, 10, RubricDimension.COMMUNICATION_CLARITY, 4));
        DesignFeedback b = fb(DesignStage.SCALING, Map.of(RubricDimension.SCALABILITY, 6, RubricDimension.DATA_MODELING, 8, RubricDimension.COMMUNICATION_CLARITY, 6));

        RubricScore r = DesignService.aggregate(List.of(a, b));
        assertThat(r.dimensions()).containsEntry(RubricDimension.SCALABILITY, 8)
                .containsEntry(RubricDimension.DATA_MODELING, 8)
                .containsEntry(RubricDimension.TRADE_OFF_REASONING, 0)
                .containsEntry(RubricDimension.COMMUNICATION_CLARITY, 5);
        assertThat(r.overall()).isEqualTo(53);   // mean(8,8,0,5)=5.25 -> 52.5 -> 53
        assertThat(r.narrative()).contains("Weakest area: trade off reasoning");
    }

    private static DesignFeedback fb(DesignStage stage, Map<RubricDimension, Integer> dims) {
        return new DesignFeedback(stage, 5, List.of(), List.of(), "n", dims, "f");
    }

    private Long id(String slug) {
        return questions.findBySlug(slug).orElseThrow().getId();
    }

    static final String STRONG_REQUIREMENTS = """
            This is read-heavy: roughly 100 redirects per write, so I'd design for reads. Assume 100M new URLs
            per month and 10k redirects per second at peak. Short codes should be 7 characters, unique, with no
            collisions because we control generation. Links can carry an expiry TTL and users may request a custom alias.
            """;
}
