package com.pokemonportfolio.pricing.service;

public record PriceRefreshSkippedItemView(
        String itemName,
        String reason) {
}
