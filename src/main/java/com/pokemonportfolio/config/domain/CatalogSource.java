package com.pokemonportfolio.config.domain;

public enum CatalogSource {
    MANUAL("Manual"),
    POKEMON_TCG_API("Pokemon TCG API");

    private final String label;

    CatalogSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
