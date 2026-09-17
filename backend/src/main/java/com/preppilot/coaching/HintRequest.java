package com.preppilot.coaching;

import com.preppilot.dsa.DifficultyTier;

/**
 * @param problemSlug   stable key of the problem (matches the problems table)
 * @param difficulty    tier the user is attempting at
 * @param attempt       user's current code / notes (may be blank)
 * @param hintDepthUsed how many hints the user has already received for this problem
 */
public record HintRequest(String problemSlug, DifficultyTier difficulty, String attempt, int hintDepthUsed) {}
