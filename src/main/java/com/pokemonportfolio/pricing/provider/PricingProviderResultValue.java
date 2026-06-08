package com.pokemonportfolio.pricing.provider;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.PricingResultType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PricingProviderResultValue(
        String providerName,
        PricingResultType resultType,
        String sourceMarket,
        BigDecimal sourcePrice,
        String sourceCurrency,
        BigDecimal exchangeRateUsed,
        BigDecimal priceSgd,
        ConfidenceRating confidenceRating,
        Integer sampleSize,
        String sourceUrl,
        OffsetDateTime sourceUpdatedAt,
        String providerMetadata) {
}
