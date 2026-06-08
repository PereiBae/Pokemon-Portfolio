package com.pokemonportfolio.pricing.provider.pokemonapi;

import com.pokemonportfolio.config.domain.CardVariant;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public record PokemonApiPricingCardView(
        Long cardId,
        String cardName,
        String expansionName,
        String cardNumber,
        String imageUrl,
        String tcgPlayerCurrency,
        BigDecimal tcgPlayerMarketPrice,
        BigDecimal tcgPlayerMidPrice,
        String cardmarketCurrency,
        BigDecimal cardmarketAveragePrice,
        BigDecimal cardmarketTrendPrice,
        BigDecimal cardmarketLowPrice,
        BigDecimal cardmarketAverageSellPrice,
        Map<CardVariant, PokemonApiRawPriceView> variantPrices,
        PokemonApiPsaPriceView psa8Price,
        PokemonApiPsaPriceView psa9Price,
        PokemonApiPsaPriceView psa10Price) {

    public PokemonApiPricingCardView(
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
        this(
                cardId,
                cardName,
                expansionName,
                cardNumber,
                imageUrl,
                tcgPlayerCurrency,
                tcgPlayerMarketPrice,
                tcgPlayerMidPrice,
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                psa8Price,
                psa9Price,
                psa10Price);
    }

    public PokemonApiPricingCardView(
            Long cardId,
            String cardName,
            String expansionName,
            String cardNumber,
            String imageUrl,
            String tcgPlayerCurrency,
            BigDecimal tcgPlayerMarketPrice,
            BigDecimal tcgPlayerMidPrice,
            Map<CardVariant, PokemonApiRawPriceView> variantPrices,
            PokemonApiPsaPriceView psa8Price,
            PokemonApiPsaPriceView psa9Price,
            PokemonApiPsaPriceView psa10Price) {
        this(
                cardId,
                cardName,
                expansionName,
                cardNumber,
                imageUrl,
                tcgPlayerCurrency,
                tcgPlayerMarketPrice,
                tcgPlayerMidPrice,
                null,
                null,
                null,
                null,
                null,
                variantPrices,
                psa8Price,
                psa9Price,
                psa10Price);
    }

    public PokemonApiPricingCardView {
        variantPrices = variantPrices == null ? Map.of() : Map.copyOf(variantPrices);
    }

    public Optional<PokemonApiRawPriceView> variantPrice(CardVariant variant) {
        if (variant == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(variantPrices.get(variant));
    }
}
