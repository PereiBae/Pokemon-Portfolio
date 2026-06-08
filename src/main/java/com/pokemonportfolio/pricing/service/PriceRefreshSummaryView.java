package com.pokemonportfolio.pricing.service;

import java.util.List;

public record PriceRefreshSummaryView(
        boolean providerEnabled,
        String providerMessage,
        int totalActiveCardsChecked,
        int snapshotsCreated,
        int exactVariantSnapshotsCreated,
        int genericFallbackSnapshotsCreated,
        int tcgPlayerUsdSnapshotsCreated,
        int cardmarketEurSnapshotsCreated,
        int skippedManualCustomCards,
        int skippedDisposedItems,
        int skippedSealedProducts,
        int skippedNoProviderMatch,
        int skippedNoMatchingVariantPrice,
        int skippedUnsafeVariant,
        int skippedNoProviderPrice,
        int skippedMissingExchangeRate,
        int skippedMissingUsdExchangeRate,
        int skippedMissingEurExchangeRate,
        int providerErrors,
        List<PriceRefreshSkippedItemView> skippedItems) {

    public PriceRefreshSummaryView {
        skippedItems = skippedItems == null ? List.of() : List.copyOf(skippedItems);
    }
}
