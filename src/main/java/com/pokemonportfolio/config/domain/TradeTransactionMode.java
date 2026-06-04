package com.pokemonportfolio.config.domain;

public enum TradeTransactionMode {
    ANALYSIS_ONLY("Analysis Only"),
    EXECUTE_TRADE("Execute Trade");

    private final String label;

    TradeTransactionMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
