package com.pokemonportfolio.pricing.provider.pokemonapi;

import java.math.BigDecimal;

public record PokemonApiRawPriceView(
        String sourceMarket,
        String sourceKey,
        String currency,
        BigDecimal marketPrice,
        BigDecimal midPrice) {

    public PokemonApiRawPriceView(
            String sourceKey,
            String currency,
            BigDecimal marketPrice,
            BigDecimal midPrice) {
        this("TCGPLAYER", sourceKey, currency, marketPrice, midPrice);
    }
}
