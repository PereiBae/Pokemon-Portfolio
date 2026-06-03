package com.pokemonportfolio.config.domain;

public enum GradedStatus {
    UNGRADED("Ungraded"),
    PSA_GRADED("PSA Graded");

    private final String label;

    GradedStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

