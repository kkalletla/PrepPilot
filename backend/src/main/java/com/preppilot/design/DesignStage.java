package com.preppilot.design;

import java.util.Optional;

/** Staged flow: requirements -> high-level components -> data model -> scaling -> complete. */
public enum DesignStage {
    REQUIREMENTS, COMPONENTS, DATA_MODEL, SCALING, COMPLETE;

    public Optional<DesignStage> next() {
        int i = ordinal() + 1;
        return i < values().length ? Optional.of(values()[i]) : Optional.empty();
    }

    public boolean isAnswerable() {
        return this != COMPLETE;
    }
}
