package com.pokemonportfolio.config.domain;

public enum OwnedItemStatus {
    ACTIVE("Active"),
    SOLD("Sold"),
    TRADED("Traded"),
    DELETED("Deleted");

    private final String label;

    OwnedItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
