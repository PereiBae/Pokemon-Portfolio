package com.pokemonportfolio.grading.service;

import com.pokemonportfolio.portfolio.entity.OwnedItem;
import java.math.BigDecimal;

public record GradingEligibleItemView(
        Long ownedItemId,
        String displayLabel,
        String imageSmallUrl,
        BigDecimal purchasePriceSgd,
        BigDecimal latestRawValueSgd) {

    public static GradingEligibleItemView from(OwnedItem item, BigDecimal latestRawValueSgd) {
        return new GradingEligibleItemView(
                item.getId(),
                item.displayName(),
                item.imageSmallUrl(),
                item.getPurchasePriceSgd(),
                latestRawValueSgd);
    }
}
