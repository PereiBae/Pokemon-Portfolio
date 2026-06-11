package com.pokemonportfolio.pricing.provider;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExternalPricingProbeCardView(
        String providerCardId,
        String cardName,
        String expansionName,
        String cardNumber,
        String languageMarket,
        String printVariant,
        String imageUrl,
        String sourceMarket,
        String sourceCurrency,
        BigDecimal rawNearMintPrice,
        BigDecimal rawLowPrice,
        BigDecimal rawAveragePrice,
        BigDecimal rawHighPrice,
        Integer saleCount,
        OffsetDateTime lastUpdatedAt,
        ExternalPricingGradedPriceView psa8Price,
        ExternalPricingGradedPriceView psa9Price,
        ExternalPricingGradedPriceView psa10Price,
        String gradedDataStatus,
        String planOrQuotaLimitation,
        String matchQuality,
        ConfidenceRating providerConfidence,
        String sourceUrl) {

    public boolean hasRawPrice() {
        return rawNearMintPrice != null || rawAveragePrice != null;
    }

    public boolean hasPsaData() {
        return psa8Price != null || psa9Price != null || psa10Price != null;
    }

    public BigDecimal displayRawPrice() {
        return rawNearMintPrice == null ? rawAveragePrice : rawNearMintPrice;
    }
}
