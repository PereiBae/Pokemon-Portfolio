package com.pokemonportfolio.pricing.service;

import java.util.List;

public record PriceHistoryChartView(
        List<PriceHistoryChartPointView> points,
        String polylinePoints,
        boolean hasEnoughData,
        String emptyMessage) {

    public boolean hasPoints() {
        return !points.isEmpty();
    }
}
