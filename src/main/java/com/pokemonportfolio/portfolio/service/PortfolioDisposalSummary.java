package com.pokemonportfolio.portfolio.service;

import java.math.BigDecimal;

public record PortfolioDisposalSummary(
        BigDecimal realizedGainLossSgd,
        BigDecimal realizedGainLossPercent,
        BigDecimal realizedCostBasisSgd) {
}
