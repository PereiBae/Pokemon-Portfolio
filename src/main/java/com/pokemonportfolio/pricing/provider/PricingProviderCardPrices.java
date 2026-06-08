package com.pokemonportfolio.pricing.provider;

import java.util.List;

public record PricingProviderCardPrices(
        PricingProviderPrice rawPrice,
        List<PricingProviderResultValue> sourceResults) {

    public PricingProviderCardPrices {
        sourceResults = sourceResults == null ? List.of() : List.copyOf(sourceResults);
    }
}
