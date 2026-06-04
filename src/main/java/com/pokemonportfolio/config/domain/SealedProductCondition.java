package com.pokemonportfolio.config.domain;

public enum SealedProductCondition {
    SEALED("Sealed"),
    DAMAGED_SEALED("Damaged Sealed"),
    OPENED("Opened"),
    OTHER("Other");

    private final String label;

    SealedProductCondition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
