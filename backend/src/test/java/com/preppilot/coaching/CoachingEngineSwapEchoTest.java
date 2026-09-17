package com.preppilot.coaching;

import static org.assertj.core.api.Assertions.assertThat;

import com.preppilot.dsa.DifficultyTier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Flipping the single config value to a different engine changes behaviour with no service/controller change —
 * proof that the abstraction is real, not interface-only (SPEC.md acceptance criteria).
 */
@SpringBootTest(properties = "coaching.engine=echo")
class CoachingEngineSwapEchoTest {

    @Autowired CoachingEngine engine;

    @Test
    void echoEngineIsActive() {
        assertThat(engine).isInstanceOf(EchoCoachingEngine.class);
        assertThat(engine.generateHint(new HintRequest("two-sum", DifficultyTier.EASY, "", 0)).hint())
                .contains(EchoCoachingEngine.MARKER);
    }
}
