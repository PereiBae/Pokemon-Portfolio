package com.pokemonportfolio.pricing.provider.pokemonapi;

import java.math.BigDecimal;

public record PokemonApiPsaPriceView(
        Integer grade,
        BigDecimal medianPrice,
        Integer sampleSize,
        String currency) {
}
