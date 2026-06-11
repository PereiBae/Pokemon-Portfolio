package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.PricingMatchClassification;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceSnapshotHistoryView(
        Long id,
        OffsetDateTime calculatedAt,
        String sourceProvider,
        String sourceMarket,
        String sourceCurrency,
        BigDecimal sourcePrice,
        BigDecimal exchangeRateUsed,
        BigDecimal marketPriceSgd,
        ConfidenceRating confidenceRating,
        String matchClassification,
        String notes,
        String sourceUrl) {

    public static PriceSnapshotHistoryView from(PriceSnapshot snapshot) {
        String match = snapshot.pricingMatchClassification()
                .map(PricingMatchClassification::getLabel)
                .orElse("Unclassified");
        return new PriceSnapshotHistoryView(
                snapshot.getId(),
                snapshot.getCalculatedAt(),
                snapshot.getProviderName(),
                snapshot.getSourceMarket() == null || snapshot.getSourceMarket().isBlank()
                        ? "Unknown market"
                        : snapshot.getSourceMarket(),
                snapshot.getSourceCurrency(),
                snapshot.getSourcePrice(),
                snapshot.getExchangeRateUsed(),
                snapshot.getMarketPriceSgd(),
                snapshot.getConfidenceRating(),
                match,
                snapshot.getExplanation(),
                snapshot.getSourceUrl());
    }

    public boolean hasSourceUrl() {
        return sourceUrl != null && !sourceUrl.isBlank();
    }

    public boolean lowConfidence() {
        return confidenceRating == ConfidenceRating.LOW;
    }

    public boolean genericFallback() {
        return "Generic raw fallback".equals(matchClassification);
    }
}
