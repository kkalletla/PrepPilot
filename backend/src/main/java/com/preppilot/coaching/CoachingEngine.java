package com.preppilot.coaching;

/**
 * The single seam between the app and any AI coaching provider.
 * Business logic (DSA hint flow, design grading) only ever talks to this interface;
 * the active implementation is chosen by the {@code coaching.engine} property.
 */
public interface CoachingEngine {

    /** Returns a graduated hint (nudge -> stronger hint -> approach outline), never full code. */
    HintResponse generateHint(HintRequest request);

    /** Grades one staged design answer, challenging trade-offs and asking a follow-up. */
    DesignFeedback gradeDesignAnswer(DesignAnswerRequest request);
}
