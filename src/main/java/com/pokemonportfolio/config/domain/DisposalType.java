package com.pokemonportfolio.config.domain;

public enum DisposalType {
    SOLD("Sold"),
    TRADED("Traded"),
    DELETED("Deleted");

    private final String label;

    DisposalType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
