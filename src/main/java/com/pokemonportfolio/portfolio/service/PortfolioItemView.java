package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import java.math.BigDecimal;

public record PortfolioItemView(
        Long ownedItemId,
        String assetTypeLabel,
        String assetName,
        String cardName,
        String setName,
        String cardNumber,
        String variant,
        String verificationStatus,
        String imageSmallUrl,
        String condition,
        BigDecimal purchasePriceSgd,
        BigDecimal marketValueSgd,
        BigDecimal gainLossSgd,
        ConfidenceRating confidenceRating,
        String sourceProvider,
        String sourceMarket,
        String sourceCurrency,
        String pricingMatchLabel,
        boolean genericRawFallbackPricing) {

    public boolean gainIsNonNegative() {
        if (gainLossSgd == null) {
            return true;
        }
        return gainLossSgd.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean hasMarketValue() {
        return marketValueSgd != null;
    }

    public boolean hasGainLoss() {
        return gainLossSgd != null;
    }

    public boolean hasPricingWarning() {
        return hasMarketValue() && genericRawFallbackPricing;
    }

    public boolean lowConfidencePricing() {
        return confidenceRating == ConfidenceRating.LOW;
    }

    public String pricingWarningLabel() {
        return genericRawFallbackPricing ? "Generic raw fallback" : "";
    }

    public String pricingWarningDetail() {
        return genericRawFallbackPricing ? "Not variant-specific" : "";
    }

    public boolean hasSourceMarket() {
        return hasMarketValue() && sourceProvider != null && !sourceProvider.isBlank();
    }

    public String pricingSourceLabel() {
        if (!hasSourceMarket()) {
            return "No snapshot";
        }
        String market = sourceMarket == null || sourceMarket.isBlank() ? "Unknown market" : sourceMarket;
        String currency = sourceCurrency == null || sourceCurrency.isBlank() ? "Unknown currency" : sourceCurrency;
        return sourceProvider + " / " + market + " / " + currency;
    }
}
