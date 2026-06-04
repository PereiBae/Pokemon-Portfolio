package com.pokemonportfolio.config.domain;

public enum TradeSideType {
    OUTGOING("Outgoing"),
    INCOMING("Incoming");

    private final String label;

    TradeSideType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
