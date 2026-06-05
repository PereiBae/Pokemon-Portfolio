package com.pokemonportfolio.config.domain;

public enum GradingCompany {
    PSA("PSA");

    private final String label;

    GradingCompany(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
