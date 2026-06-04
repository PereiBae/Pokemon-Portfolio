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
        ConfidenceRating confidenceRating) {

    public boolean gainIsNonNegative() {
        return gainLossSgd.compareTo(BigDecimal.ZERO) >= 0;
    }
}
