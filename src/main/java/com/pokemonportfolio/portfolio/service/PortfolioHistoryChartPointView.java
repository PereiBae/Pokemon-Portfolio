package com.pokemonportfolio.portfolio.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PortfolioHistoryChartPointView(
        OffsetDateTime calculatedAt,
        BigDecimal valueSgd,
        String label,
        String coordinate) {
}
