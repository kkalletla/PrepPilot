package com.preppilot.dsa;

import java.util.Optional;

/** Ordered difficulty ladder: EASY -> MEDIUM -> HARD -> FAANG_BAR. */
public enum DifficultyTier {
    EASY, MEDIUM, HARD, FAANG_BAR;

    public Optional<DifficultyTier> next() {
        int i = ordinal() + 1;
        return i < values().length ? Optional.of(values()[i]) : Optional.empty();
    }
}
