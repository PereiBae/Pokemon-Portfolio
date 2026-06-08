package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.alerts.service.AlertView;
import java.math.BigDecimal;
import java.util.List;

public record PortfolioDashboardView(
        BigDecimal totalValueSgd,
        BigDecimal totalCostBasisSgd,
        BigDecimal unrealizedGainLossSgd,
        BigDecimal unrealizedGainLossPercent,
        int itemCount,
        int lowConfidenceCount,
        List<PortfolioItemView> items,
        BigDecimal realizedGainLossSgd,
        BigDecimal realizedGainLossPercent,
        BigDecimal realizedCostBasisSgd,
        BigDecimal totalPerformanceSgd,
        BigDecimal totalPerformancePercent,
        int activeAlertCount,
        List<AlertView> latestAlerts) {

    public PortfolioDashboardView(
            BigDecimal totalValueSgd,
            BigDecimal totalCostBasisSgd,
            BigDecimal unrealizedGainLossSgd,
            BigDecimal unrealizedGainLossPercent,
            int itemCount,
            int lowConfidenceCount,
            List<PortfolioItemView> items) {
        this(
                totalValueSgd,
                totalCostBasisSgd,
                unrealizedGainLossSgd,
                unrealizedGainLossPercent,
                itemCount,
                lowConfidenceCount,
                items,
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(4),
                BigDecimal.ZERO.setScale(2),
                unrealizedGainLossSgd,
                unrealizedGainLossPercent,
                0,
                List.of());
    }

    public boolean gainIsNonNegative() {
        return unrealizedGainLossSgd.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean hasPricedItems() {
        return items.stream().anyMatch(PortfolioItemView::hasMarketValue);
    }

    public boolean activeValueUnavailable() {
        return itemCount > 0 && !hasPricedItems();
    }

    public PortfolioDashboardView withAlerts(int activeAlertCount, List<AlertView> latestAlerts) {
        return new PortfolioDashboardView(
                totalValueSgd,
                totalCostBasisSgd,
                unrealizedGainLossSgd,
                unrealizedGainLossPercent,
                itemCount,
                lowConfidenceCount,
                items,
                realizedGainLossSgd,
                realizedGainLossPercent,
                realizedCostBasisSgd,
                totalPerformanceSgd,
                totalPerformancePercent,
                activeAlertCount,
                List.copyOf(latestAlerts));
    }

    public boolean realizedGainIsNonNegative() {
        return realizedGainLossSgd.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean totalPerformanceIsNonNegative() {
        return totalPerformanceSgd.compareTo(BigDecimal.ZERO) >= 0;
    }

    public BigDecimal activePortfolioValueSgd() {
        return totalValueSgd;
    }

    public BigDecimal activeCostBasisSgd() {
        return totalCostBasisSgd;
    }
}
