package com.pokemonportfolio.config.domain;

public enum CardVariant {
    STANDARD("Standard"),
    REVERSE_HOLO("Reverse Holo"),
    HOLO("Holo"),
    FIRST_EDITION_HOLO("1st Edition Holo"),
    FIRST_EDITION_NORMAL("1st Edition Normal"),
    UNLIMITED_HOLO("Unlimited Holo"),
    UNLIMITED_NORMAL("Unlimited Normal"),
    PROMO("Promo"),
    STAMPED("Stamped"),
    ALTERNATE_ART("Alternate Art"),
    SECRET_RARE("Secret Rare"),
    MASTER_BALL("Master Ball"),
    POKE_BALL("Poke Ball"),
    ERROR("Error");

    private final String label;

    CardVariant(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
