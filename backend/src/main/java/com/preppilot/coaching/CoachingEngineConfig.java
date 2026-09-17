package com.preppilot.coaching;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the active {@link CoachingEngine} from {@code coaching.engine=template|claude|openai}.
 * Adding a live-model engine means adding another conditional bean here — no service or controller changes.
 */
@Configuration
public class CoachingEngineConfig {

    @Bean
    @ConditionalOnProperty(name = "coaching.engine", havingValue = "template", matchIfMissing = true)
    public CoachingEngine templateCoachingEngine(ObjectMapper mapper) {
        return TemplateCoachingEngine.fromClasspath(mapper);
    }
}
