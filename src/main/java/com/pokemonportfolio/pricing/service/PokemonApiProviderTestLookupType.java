package com.pokemonportfolio.pricing.service;

public enum PokemonApiProviderTestLookupType {
    CARD("Card"),
    SEALED_PRODUCT("Sealed Product");

    private final String label;

    PokemonApiProviderTestLookupType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
