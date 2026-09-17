package com.preppilot.coaching;

import com.preppilot.design.DesignStage;
import java.util.List;
import java.util.Map;

/**
 * Structured feedback for one stage.
 *
 * @param stage           stage that was graded
 * @param stageScore      0-10 for this stage
 * @param strengths       concepts the answer covered well
 * @param gaps            concepts the answer missed
 * @param narrative       short human-readable feedback
 * @param dimensionScores 0-10 per rubric dimension this stage informs (absent dimensions are not scored here)
 * @param followUpPrompt  the coach's next challenge / question
 */
public record DesignFeedback(
        DesignStage stage,
        int stageScore,
        List<String> strengths,
        List<String> gaps,
        String narrative,
        Map<RubricDimension, Integer> dimensionScores,
        String followUpPrompt) {}
