package com.preppilot.coaching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppilot.design.DesignStage;
import com.preppilot.dsa.DifficultyTier;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TemplateCoachingEngineTest {

    static TemplateCoachingEngine engine;

    @BeforeAll
    static void load() {
        engine = TemplateCoachingEngine.fromClasspath(new ObjectMapper());
    }

    @Test
    void bundledTemplatesCoverTheStarterBanks() {
        assertThat(engine.problemSlugs()).hasSizeGreaterThanOrEqualTo(10);
        assertThat(engine.questionSlugs()).containsExactlyInAnyOrder("url-shortener", "rate-limiter", "chat-application", "news-feed");
    }

    @Test
    void hintsAreGraduatedAndStopAtTheOutline() {
        HintResponse h1 = engine.generateHint(new HintRequest("two-sum", DifficultyTier.EASY, "", 0));
        HintResponse h2 = engine.generateHint(new HintRequest("two-sum", DifficultyTier.EASY, "", 1));
        HintResponse h3 = engine.generateHint(new HintRequest("two-sum", DifficultyTier.EASY, "", 2));
        HintResponse h4 = engine.generateHint(new HintRequest("two-sum", DifficultyTier.EASY, "", 7));

        assertThat(h1.depth()).isEqualTo(1);
        assertThat(h2.depth()).isEqualTo(2);
        assertThat(h3.depth()).isEqualTo(3);
        assertThat(h3.exhausted()).isTrue();
        assertThat(h1.exhausted()).isFalse();
        assertThat(h4.hint()).isEqualTo(h3.hint());                // past the end keeps returning the outline
        assertThat(List.of(h1.hint(), h2.hint(), h3.hint())).doesNotHaveDuplicates();
        assertThat(h1.followUpPrompt()).isNotBlank();
    }

    @Test
    void hintsNeverContainFullCode() {
        for (String slug : engine.problemSlugs()) {
            for (int depth = 0; depth < 3; depth++) {
                String hint = engine.generateHint(new HintRequest(slug, DifficultyTier.EASY, "", depth)).hint();
                assertThat(hint).as(slug + " depth " + depth)
                        .doesNotContain("```").doesNotContain("def ").doesNotContain("public static")
                        .doesNotContain("return ").doesNotContain("for (");
            }
        }
    }

    @Test
    void unknownProblemIsRejected() {
        assertThatThrownBy(() -> engine.generateHint(new HintRequest("nope", DifficultyTier.EASY, "", 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void strongAnswerScoresHighAndGetsTradeOffFollowUp() {
        String answer = """
                This is read-heavy: roughly 100 redirects per write, so I'd design for reads. Assume 100M new URLs
                per month and 10k redirects per second at peak. Short codes should be 7 characters, unique, with no
                collisions because we control generation. Links can carry an expiry TTL and users may request a custom alias,
                which I'd support rather than reject because it's a common product need.
                """;
        DesignFeedback fb = engine.gradeDesignAnswer(new DesignAnswerRequest("url-shortener", DesignStage.REQUIREMENTS, answer, List.of()));

        assertThat(fb.stageScore()).isEqualTo(10);
        assertThat(fb.gaps()).isEmpty();
        assertThat(fb.strengths()).hasSize(4);
        assertThat(fb.followUpPrompt()).contains("latency target");
        assertThat(fb.dimensionScores()).containsKeys(RubricDimension.SCALABILITY, RubricDimension.DATA_MODELING,
                RubricDimension.TRADE_OFF_REASONING, RubricDimension.COMMUNICATION_CLARITY);
        assertThat(fb.dimensionScores().get(RubricDimension.COMMUNICATION_CLARITY)).isGreaterThanOrEqualTo(7);
    }

    @Test
    void weakAnswerIsChallengedOnTheFirstGap() {
        DesignFeedback fb = engine.gradeDesignAnswer(new DesignAnswerRequest("url-shortener", DesignStage.REQUIREMENTS, "use a database", List.of()));

        assertThat(fb.stageScore()).isZero();
        assertThat(fb.gaps()).hasSize(4);
        assertThat(fb.followUpPrompt()).contains("read/write ratio");
        assertThat(fb.narrative()).contains("Missing");
        assertThat(fb.dimensionScores().get(RubricDimension.COMMUNICATION_CLARITY)).isLessThanOrEqualTo(2);
    }

    @Test
    void everyQuestionHasAllFourGradedStages() {
        for (String slug : engine.questionSlugs()) {
            for (DesignStage stage : TemplateCoachingEngine.gradedStages()) {
                DesignFeedback fb = engine.gradeDesignAnswer(new DesignAnswerRequest(slug, stage, "", List.of()));
                assertThat(fb.followUpPrompt()).as(slug + "/" + stage).isNotBlank().doesNotContain("{gap}");
            }
            assertThat(engine.designIntro(slug)).isNotBlank();
        }
    }

    @Test
    void clarityRewardsSubstanceStructureAndJustification() {
        assertThat(TemplateCoachingEngine.clarityScore("")).isZero();
        assertThat(TemplateCoachingEngine.clarityScore("Use Redis.")).isZero();
        String structured = "First, clarify the read path. Second, size the cache because reads dominate. Third, plan for eviction.";
        assertThat(TemplateCoachingEngine.clarityScore(structured)).isGreaterThanOrEqualTo(8);
    }
}
