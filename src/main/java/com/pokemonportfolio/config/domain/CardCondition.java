package com.pokemonportfolio.config.domain;

public enum CardCondition {
    RAW_NEAR_MINT("Near Mint"),
    RAW_LIGHTLY_PLAYED("Lightly Played"),
    RAW_MODERATELY_PLAYED("Moderately Played"),
    RAW_HEAVILY_PLAYED("Heavily Played"),
    RAW_DAMAGED("Damaged");

    private final String label;

    CardCondition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

