package com.pokemonportfolio.pricing.provider;

import java.math.BigDecimal;

public record ExternalPricingGradedPriceView(
        String grade,
        BigDecimal averagePrice,
        BigDecimal medianPrice,
        BigDecimal smartMarketPrice,
        Integer sampleSize,
        String currency) {

    public BigDecimal displayPrice() {
        if (smartMarketPrice != null) {
            return smartMarketPrice;
        }
        if (medianPrice != null) {
            return medianPrice;
        }
        return averagePrice;
    }
}
