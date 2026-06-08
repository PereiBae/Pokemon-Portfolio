package com.pokemonportfolio.pricing.provider;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExchangeRateQuote(
        String sourceCurrency,
        String targetCurrency,
        BigDecimal exchangeRate,
        String rateSource,
        OffsetDateTime effectiveAt,
        OffsetDateTime fetchedAt) {
}
