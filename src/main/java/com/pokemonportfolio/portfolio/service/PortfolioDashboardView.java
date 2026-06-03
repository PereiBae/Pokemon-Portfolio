package com.pokemonportfolio.portfolio.service;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioDashboardView(
        BigDecimal totalValueSgd,
        BigDecimal totalCostBasisSgd,
        BigDecimal unrealizedGainLossSgd,
        BigDecimal unrealizedGainLossPercent,
        int itemCount,
        int lowConfidenceCount,
        List<PortfolioItemView> items) {

    public boolean gainIsNonNegative() {
        return unrealizedGainLossSgd.compareTo(BigDecimal.ZERO) >= 0;
    }
}

