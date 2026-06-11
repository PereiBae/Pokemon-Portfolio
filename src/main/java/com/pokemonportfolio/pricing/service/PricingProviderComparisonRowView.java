package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.pricing.provider.ExternalPricingProbeCardView;
import java.math.BigDecimal;

public record PricingProviderComparisonRowView(
        String providerName,
        ExternalPricingProbeCardView card,
        BigDecimal convertedSgdPreview,
        String exchangeRateNote,
        String providerError) {

    public boolean hasResult() {
        return card != null;
    }
}
