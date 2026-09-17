package com.preppilot.coaching;

import static org.assertj.core.api.Assertions.assertThat;

import com.preppilot.dsa.DifficultyTier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** With coaching.engine=template the curated engine answers. Pair with {@link CoachingEngineSwapEchoTest}. */
@SpringBootTest(properties = "coaching.engine=template")
class CoachingEngineSwapTemplateTest {

    @Autowired CoachingEngine engine;

    @Test
    void templateEngineIsActive() {
        assertThat(engine).isInstanceOf(TemplateCoachingEngine.class);
        assertThat(engine.generateHint(new HintRequest("two-sum", DifficultyTier.EASY, "", 0)).hint())
                .doesNotContain(EchoCoachingEngine.MARKER);
    }
}
