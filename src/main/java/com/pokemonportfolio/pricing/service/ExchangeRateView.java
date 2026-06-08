package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExchangeRateView(
        Long id,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal exchangeRate,
        String rateSource,
        ConfidenceRating confidenceRating,
        OffsetDateTime effectiveAt,
        OffsetDateTime fetchedAt,
        boolean active) {
}
