package com.pokemonportfolio.config.domain;

public enum AlertStatus {
    NEW("New"),
    ACTIVE("Active"),
    ACKNOWLEDGED("Acknowledged"),
    DISMISSED("Dismissed");

    private final String label;

    AlertStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
