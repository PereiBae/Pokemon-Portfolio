package com.pokemonportfolio.pricing.provider;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.CardVariant;
import java.util.List;

public interface PricingProviderAdapter {

    String providerName();

    boolean isEnabled();

    PricingProviderPrice fetchCardPrice(Card card);

    default PricingProviderPrice fetchCardPrice(Card card, CardVariant variant) {
        return fetchCardPrice(card);
    }

    default PricingProviderCardPrices fetchCardPrices(Card card, CardVariant variant) {
        PricingProviderPrice price = fetchCardPrice(card, variant);
        return new PricingProviderCardPrices(price, List.of(new PricingProviderResultValue(
                price.providerName(),
                com.pokemonportfolio.config.domain.PricingResultType.RAW_CARD,
                price.sourceMarket(),
                price.sourcePrice(),
                price.sourceCurrency(),
                price.exchangeRateUsed(),
                price.marketPriceSgd(),
                price.confidenceRating(),
                null,
                price.sourceUrl(),
                price.sourceUpdatedAt(),
                price.providerMetadata())));
    }

    default String unavailableMessage() {
        return providerName() + " is unavailable.";
    }
}
