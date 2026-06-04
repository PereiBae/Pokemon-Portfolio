package com.pokemonportfolio.trade.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.TradeSideType;
import java.math.BigDecimal;

public record TradeItemView(
        Long tradeItemId,
        TradeSideType sideType,
        String itemDisplayName,
        String imageSmallUrl,
        String variantLabel,
        String conditionLabel,
        BigDecimal marketValueSgd,
        BigDecimal overrideValueSgd,
        BigDecimal baseValueSgd,
        BigDecimal adjustedValueSgd,
        BigDecimal allocatedCostBasisSgd,
        ConfidenceRating confidenceRating,
        Long outgoingOwnedItemId,
        Long incomingOwnedItemId,
        Long disposalId,
        String notes) {

    public boolean hasAllocatedCostBasis() {
        return allocatedCostBasisSgd != null;
    }

    public boolean hasOverrideValue() {
        return overrideValueSgd != null;
    }

    public BigDecimal agreedValueSgd() {
        return baseValueSgd;
    }
}
