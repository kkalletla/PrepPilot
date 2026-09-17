package com.preppilot.coaching;

/**
 * @param hint           the hint text for this depth
 * @param depth          1-based depth of the hint returned
 * @param maxDepth       total hint depths available for the problem
 * @param exhausted      true when no deeper hint exists beyond this one
 * @param followUpPrompt optional Socratic question to keep the user thinking
 */
public record HintResponse(String hint, int depth, int maxDepth, boolean exhausted, String followUpPrompt) {}
