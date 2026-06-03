package com.pokemonportfolio.pricing.provider;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import java.math.BigDecimal;

public record PricingProviderPrice(
        String providerName,
        BigDecimal sourcePrice,
        String sourceCurrency,
        BigDecimal exchangeRateUsed,
        BigDecimal marketPriceSgd,
        ConfidenceRating confidenceRating,
        String explanation) {
}

