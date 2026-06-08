package com.pokemonportfolio.config.domain;

public enum PricingResultType {
    RAW_CARD("Raw Card"),
    PSA_8("PSA 8"),
    PSA_9("PSA 9"),
    PSA_10("PSA 10");

    private final String label;

    PricingResultType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
