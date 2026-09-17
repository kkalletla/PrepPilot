package com.preppilot.coaching;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/** Jackson-bound shape of the curated template files under {@code classpath:coaching/}. */
public final class CoachingTemplates {
    private CoachingTemplates() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HintTemplate(List<String> hints, String followUp) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Concept(String label, List<String> aliases, RubricDimension dimension) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StageTemplate(List<Concept> concepts, String followUp, String challenge) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DesignTemplate(String intro, Map<String, StageTemplate> stages) {}
}
