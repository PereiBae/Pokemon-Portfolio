package com.pokemonportfolio.config.domain;

public enum GradingScenarioType {
    CONSERVATIVE("Conservative"),
    BALANCED("Balanced"),
    AGGRESSIVE("Aggressive");

    private final String label;

    GradingScenarioType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
