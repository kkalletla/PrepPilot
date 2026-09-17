package com.preppilot.coaching;

import com.preppilot.design.DesignStage;

/** One line of a staged design conversation; stored as plain data, independent of any model provider. */
public record TranscriptTurn(Role role, DesignStage stage, String content) {
    public enum Role { COACH, CANDIDATE }
}
