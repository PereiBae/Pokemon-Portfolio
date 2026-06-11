package com.pokemonportfolio.pricing.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceHistoryChartPointView(
        OffsetDateTime calculatedAt,
        BigDecimal marketPriceSgd,
        String label,
        String coordinate) {
}
