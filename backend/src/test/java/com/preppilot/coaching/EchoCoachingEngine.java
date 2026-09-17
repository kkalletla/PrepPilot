package com.preppilot.coaching;

import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test-only second implementation. Exists purely to prove that flipping {@code coaching.engine}
 * swaps the active engine without touching any service or controller (SPEC.md acceptance criteria).
 */
public class EchoCoachingEngine implements CoachingEngine {

    public static final String MARKER = "[echo-engine]";

    @Override
    public HintResponse generateHint(HintRequest request) {
        return new HintResponse(MARKER + " " + request.problemSlug(), 1, 1, true, null);
    }

    @Override
    public DesignFeedback gradeDesignAnswer(DesignAnswerRequest request) {
        return new DesignFeedback(request.stage(), 5, List.of(), List.of(), MARKER, Map.of(), MARKER);
    }

    @Configuration
    public static class Config {
        @Bean
        @ConditionalOnProperty(name = "coaching.engine", havingValue = "echo")
        CoachingEngine echoCoachingEngine() {
            return new EchoCoachingEngine();
        }
    }
}
