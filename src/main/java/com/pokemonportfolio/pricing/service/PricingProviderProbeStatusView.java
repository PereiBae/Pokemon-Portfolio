package com.pokemonportfolio.pricing.service;

public record PricingProviderProbeStatusView(
        String providerName,
        boolean enabled,
        boolean apiKeyConfigured,
        String baseUrl,
        String apiKeyLabel) {
}
