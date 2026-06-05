package com.pokemonportfolio.pricing.provider.pokemonapi;

import java.math.BigDecimal;

public record PokemonApiSealedProductPriceView(
        Long productId,
        String productName,
        String expansionName,
        String imageUrl,
        String cardmarketCurrency,
        BigDecimal cardmarketLowestPrice,
        BigDecimal cardmarketLowestDePrice,
        BigDecimal cardmarketLowestFrPrice,
        BigDecimal cardmarketLowestEsPrice,
        BigDecimal cardmarketLowestItPrice,
        String tcgPlayerCurrency,
        BigDecimal tcgPlayerMarketPrice,
        BigDecimal tcgPlayerMidPrice) {
}
