package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.pricing.provider.ExternalPricingProbeCardView;

public record PricingProviderProbeResultView(
        String sourceProvider,
        String sourceUrl,
        ExternalPricingProbeCardView card) {
}
