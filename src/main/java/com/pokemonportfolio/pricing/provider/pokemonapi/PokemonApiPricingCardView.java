package com.pokemonportfolio.pricing.provider.pokemonapi;

import java.math.BigDecimal;

public record PokemonApiPricingCardView(
        Long cardId,
        String cardName,
        String expansionName,
        String cardNumber,
        String imageUrl,
        String tcgPlayerCurrency,
        BigDecimal tcgPlayerMarketPrice,
        BigDecimal tcgPlayerMidPrice,
        PokemonApiPsaPriceView psa8Price,
        PokemonApiPsaPriceView psa9Price,
        PokemonApiPsaPriceView psa10Price) {
}
