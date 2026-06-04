package com.pokemonportfolio.config.domain;

public enum AssetType {
    CARD("Card"),
    SEALED_PRODUCT("Sealed Product");

    private final String label;

    AssetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
