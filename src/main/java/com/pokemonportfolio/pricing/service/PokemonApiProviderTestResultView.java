package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingCardView;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiSealedProductPriceView;

public record PokemonApiProviderTestResultView(
        PokemonApiProviderTestLookupType lookupType,
        String sourceProvider,
        String sourceUrl,
        PokemonApiPricingCardView card,
        PokemonApiSealedProductPriceView sealedProduct) {

    public boolean isCardResult() {
        return lookupType == PokemonApiProviderTestLookupType.CARD && card != null;
    }

    public boolean isSealedProductResult() {
        return lookupType == PokemonApiProviderTestLookupType.SEALED_PRODUCT && sealedProduct != null;
    }
}
