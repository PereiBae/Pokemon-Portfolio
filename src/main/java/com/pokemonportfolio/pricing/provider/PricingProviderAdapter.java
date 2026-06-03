package com.pokemonportfolio.pricing.provider;

import com.pokemonportfolio.catalog.entity.Card;

public interface PricingProviderAdapter {

    String providerName();

    boolean isEnabled();

    PricingProviderPrice fetchCardPrice(Card card);
}

