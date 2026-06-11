package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.config.domain.HistoryRange;
import java.util.List;

public record PortfolioHistoryPageView(
        HistoryRange selectedRange,
        List<HistoryRange> rangeOptions,
        List<PortfolioHistorySnapshotView> snapshots,
        PortfolioHistoryChartView chart,
        PortfolioDashboardView currentValuation) {

    public boolean hasSnapshots() {
        return !snapshots.isEmpty();
    }

    public boolean hasLowDataState() {
        return !chart.hasEnoughData();
    }
}
