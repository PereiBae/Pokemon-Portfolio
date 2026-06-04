package com.pokemonportfolio.config.domain;

public enum TradeFairnessResult {
    FAVORABLE("Favorable"),
    BALANCED("Balanced"),
    UNFAVORABLE("Unfavorable");

    private final String label;

    TradeFairnessResult(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
