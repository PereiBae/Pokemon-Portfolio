package com.pokemonportfolio.pricing.provider;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.PricingMatchClassification;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

public record PricingProviderPrice(
        String providerName,
        String sourceMarket,
        BigDecimal sourcePrice,
        String sourceCurrency,
        BigDecimal exchangeRateUsed,
        BigDecimal marketPriceSgd,
        ConfidenceRating confidenceRating,
        String sourceUrl,
        OffsetDateTime sourceUpdatedAt,
        String providerMetadata,
        String explanation) {

    public PricingProviderPrice(
            String providerName,
            BigDecimal sourcePrice,
            String sourceCurrency,
            BigDecimal exchangeRateUsed,
            BigDecimal marketPriceSgd,
            ConfidenceRating confidenceRating,
            String explanation) {
        this(
                providerName,
                null,
                sourcePrice,
                sourceCurrency,
                exchangeRateUsed,
                marketPriceSgd,
                confidenceRating,
                null,
                null,
                null,
                explanation);
    }

    public Optional<PricingMatchClassification> pricingMatchClassification() {
        return PricingMatchClassification.fromMetadata(providerMetadata);
    }
}
