package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.HistoryRange;
import java.math.BigDecimal;
import java.util.List;

public record OwnedItemPriceHistoryPageView(
        Long ownedItemId,
        String assetTypeLabel,
        String itemName,
        String setOrProductType,
        String variant,
        String condition,
        String imageUrl,
        BigDecimal purchasePriceSgd,
        BigDecimal latestMarketValueSgd,
        BigDecimal gainLossSgd,
        BigDecimal gainLossPercent,
        ConfidenceRating latestConfidence,
        String latestSourceProvider,
        String latestSourceMarket,
        String latestSourceCurrency,
        HistoryRange selectedRange,
        List<HistoryRange> rangeOptions,
        List<PriceSnapshotHistoryView> snapshots,
        PriceHistoryChartView chart) {

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }

    public boolean hasLatestMarketValue() {
        return latestMarketValueSgd != null;
    }

    public boolean hasGainLoss() {
        return gainLossSgd != null;
    }

    public boolean gainIsNonNegative() {
        return gainLossSgd == null || gainLossSgd.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean hasSnapshots() {
        return !snapshots.isEmpty();
    }

    public String latestSourceLabel() {
        if (!hasLatestMarketValue()) {
            return "No snapshot";
        }
        String market = latestSourceMarket == null || latestSourceMarket.isBlank() ? "Unknown market" : latestSourceMarket;
        String currency = latestSourceCurrency == null || latestSourceCurrency.isBlank() ? "Unknown currency" : latestSourceCurrency;
        return latestSourceProvider + " / " + market + " / " + currency;
    }
}
