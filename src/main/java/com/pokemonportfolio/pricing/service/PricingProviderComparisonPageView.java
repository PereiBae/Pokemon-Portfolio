package com.pokemonportfolio.pricing.service;

import java.util.List;

public record PricingProviderComparisonPageView(
        String searchOrId,
        List<PricingProviderComparisonRowView> rows) {
}
