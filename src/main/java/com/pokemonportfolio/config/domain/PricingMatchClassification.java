package com.pokemonportfolio.config.domain;

import java.util.Optional;

public enum PricingMatchClassification {
    EXACT_VARIANT_MATCH("Exact variant match"),
    GENERIC_RAW_FALLBACK("Generic raw fallback"),
    UNSAFE_VARIANT_MISMATCH("Unsafe variant mismatch"),
    NO_PRICE_AVAILABLE("No price available");

    private final String label;

    PricingMatchClassification(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<PricingMatchClassification> fromMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Optional.empty();
        }
        for (PricingMatchClassification classification : values()) {
            if (metadata.contains("match=" + classification.name())) {
                return Optional.of(classification);
            }
        }
        return Optional.empty();
    }
}
