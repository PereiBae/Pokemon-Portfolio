package com.pokemonportfolio.config.domain;

public enum LanguageMarket {
    ENGLISH("English"),
    JAPANESE("Japanese"),
    CHINESE("Chinese"),
    OTHER("Other");

    private final String label;

    LanguageMarket(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
