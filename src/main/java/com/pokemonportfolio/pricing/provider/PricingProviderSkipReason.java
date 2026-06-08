package com.pokemonportfolio.pricing.provider;

public enum PricingProviderSkipReason {
    NO_PROVIDER_MATCH,
    NO_MATCHING_VARIANT_PRICE,
    UNSAFE_VARIANT_MISMATCH,
    NO_PRICE_AVAILABLE,
    MISSING_EXCHANGE_RATE,
    PROVIDER_ERROR
}
