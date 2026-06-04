package com.pokemonportfolio.config.domain;

public enum TradeTransactionStatus {
    DRAFT("Draft"),
    ANALYSED("Analysed"),
    EXECUTED("Executed"),
    CANCELLED("Cancelled");

    private final String label;

    TradeTransactionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
