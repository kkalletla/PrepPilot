package com.preppilot.coaching;

import com.preppilot.design.DesignStage;
import java.util.List;

/**
 * @param questionSlug stable key of the design question
 * @param stage        which stage of the flow the answer belongs to
 * @param answer       the user's staged design input
 * @param transcript   conversation so far (both roles), oldest first
 */
public record DesignAnswerRequest(String questionSlug, DesignStage stage, String answer, List<TranscriptTurn> transcript) {}
