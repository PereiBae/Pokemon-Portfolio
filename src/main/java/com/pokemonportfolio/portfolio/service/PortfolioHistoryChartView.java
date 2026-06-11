package com.pokemonportfolio.portfolio.service;

import java.util.List;

public record PortfolioHistoryChartView(
        List<PortfolioHistoryChartPointView> points,
        String polylinePoints,
        boolean hasEnoughData,
        String emptyMessage) {

    public boolean hasPoints() {
        return !points.isEmpty();
    }
}
