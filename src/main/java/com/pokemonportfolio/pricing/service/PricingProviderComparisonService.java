package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.provider.ExternalPricingGradedPriceView;
import com.pokemonportfolio.pricing.provider.ExternalPricingProbeCardView;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingCardView;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProperties;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProvider;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProviderException;
import com.pokemonportfolio.pricing.provider.poketrace.PokeTracePricingProvider;
import com.pokemonportfolio.pricing.provider.poketrace.PokeTracePricingProviderException;
import com.pokemonportfolio.pricing.provider.pokemonpricetracker.PokemonPriceTrackerProvider;
import com.pokemonportfolio.pricing.provider.pokemonpricetracker.PokemonPriceTrackerProviderException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PricingProviderComparisonService {

    private final PokemonApiPricingProperties pokemonApiProperties;
    private final PokemonApiPricingProvider pokemonApiProvider;
    private final PokeTracePricingProvider pokeTraceProvider;
    private final PokemonPriceTrackerProvider pokemonPriceTrackerProvider;
    private final CurrencyConversionService currencyConversionService;

    public PricingProviderComparisonService(
            PokemonApiPricingProperties pokemonApiProperties,
            PokemonApiPricingProvider pokemonApiProvider,
            PokeTracePricingProvider pokeTraceProvider,
            PokemonPriceTrackerProvider pokemonPriceTrackerProvider,
            CurrencyConversionService currencyConversionService) {
        this.pokemonApiProperties = pokemonApiProperties;
        this.pokemonApiProvider = pokemonApiProvider;
        this.pokeTraceProvider = pokeTraceProvider;
        this.pokemonPriceTrackerProvider = pokemonPriceTrackerProvider;
        this.currencyConversionService = currencyConversionService;
    }

    public PricingProviderComparisonPageView compare(PricingProviderComparisonForm form) {
        String searchOrId = requireSearchOrId(form.getSearchOrId());
        List<PricingProviderComparisonRowView> rows = new ArrayList<>();
        rows.add(pokemonApiRow(searchOrId));
        rows.add(pokeTraceRow(searchOrId));
        rows.add(pokemonPriceTrackerRow(searchOrId));
        return new PricingProviderComparisonPageView(searchOrId, rows);
    }

    private PricingProviderComparisonRowView pokemonApiRow(String searchOrId) {
        if (!pokemonApiProperties.isEnabled()) {
            return error("PokemonApiPricingProvider", "Pokemon API provider is disabled.");
        }
        if (pokemonApiProperties.getRapidApiKey() == null || pokemonApiProperties.getRapidApiKey().isBlank()) {
            return error("PokemonApiPricingProvider", "Pokemon API RapidAPI key is missing.");
        }
        try {
            PokemonApiPricingCardView card = pokemonApiProvider.fetchOneCard(searchOrId);
            return row("PokemonApiPricingProvider", toExternalCard(card), null);
        } catch (PokemonApiPricingProviderException ex) {
            return error("PokemonApiPricingProvider", ex.getMessage());
        } catch (RuntimeException ex) {
            return error("PokemonApiPricingProvider", "Pokemon API comparison request failed.");
        }
    }

    private PricingProviderComparisonRowView pokeTraceRow(String searchOrId) {
        try {
            return row(pokeTraceProvider.providerName(), pokeTraceProvider.fetchOneCard(searchOrId), null);
        } catch (PokeTracePricingProviderException ex) {
            return error(pokeTraceProvider.providerName(), ex.getMessage());
        } catch (RuntimeException ex) {
            return error(pokeTraceProvider.providerName(), "PokeTrace comparison request failed.");
        }
    }

    private PricingProviderComparisonRowView pokemonPriceTrackerRow(String searchOrId) {
        try {
            return row(
                    pokemonPriceTrackerProvider.providerName(),
                    pokemonPriceTrackerProvider.fetchOneCard(searchOrId),
                    null);
        } catch (PokemonPriceTrackerProviderException ex) {
            return error(pokemonPriceTrackerProvider.providerName(), ex.getMessage());
        } catch (RuntimeException ex) {
            return error(pokemonPriceTrackerProvider.providerName(), "PokemonPriceTracker comparison request failed.");
        }
    }

    private PricingProviderComparisonRowView row(String providerName, ExternalPricingProbeCardView card, String error) {
        if (card == null) {
            return new PricingProviderComparisonRowView(providerName, null, null, null, error);
        }
        return new PricingProviderComparisonRowView(
                providerName,
                card,
                convertedSgd(card),
                exchangeRateNote(card),
                error);
    }

    private PricingProviderComparisonRowView error(String providerName, String message) {
        return new PricingProviderComparisonRowView(providerName, null, null, null, message);
    }

    private ExternalPricingProbeCardView toExternalCard(PokemonApiPricingCardView card) {
        BigDecimal rawPrice = card.tcgPlayerMarketPrice() != null
                ? card.tcgPlayerMarketPrice()
                : card.tcgPlayerMidPrice();
        String sourceMarket = rawPrice != null ? "TCGPLAYER" : null;
        String sourceCurrency = rawPrice != null ? card.tcgPlayerCurrency() : null;
        if (rawPrice == null) {
            rawPrice = card.cardmarketAveragePrice() != null
                    ? card.cardmarketAveragePrice()
                    : card.cardmarketTrendPrice();
            sourceMarket = rawPrice == null ? null : "CARDMARKET";
            sourceCurrency = rawPrice == null ? null : card.cardmarketCurrency();
        }
        return new ExternalPricingProbeCardView(
                card.cardId() == null ? null : card.cardId().toString(),
                card.cardName(),
                card.expansionName(),
                card.cardNumber(),
                "ENGLISH",
                null,
                card.imageUrl(),
                sourceMarket,
                sourceCurrency,
                rawPrice,
                card.cardmarketLowPrice(),
                rawPrice,
                null,
                null,
                null,
                psa(card.psa8Price()),
                psa(card.psa9Price()),
                psa(card.psa10Price()),
                card.psa8Price() != null || card.psa9Price() != null || card.psa10Price() != null
                        ? "Graded data returned successfully."
                        : "No PSA prices mapped.",
                null,
                "Existing pokemon-api.com provider match rules",
                rawPrice == null ? ConfidenceRating.LOW : ConfidenceRating.MEDIUM,
                sourceUrl("/cards/" + card.cardId()));
    }

    private ExternalPricingGradedPriceView psa(com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPsaPriceView price) {
        if (price == null) {
            return null;
        }
        return new ExternalPricingGradedPriceView(
                "PSA " + price.grade(),
                null,
                price.medianPrice(),
                null,
                price.sampleSize(),
                price.currency());
    }

    private String sourceUrl(String path) {
        String baseUrl = pokemonApiProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return path;
        }
        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) + path
                : baseUrl + path;
    }

    private BigDecimal convertedSgd(ExternalPricingProbeCardView card) {
        if (card.displayRawPrice() == null || card.sourceCurrency() == null) {
            return null;
        }
        try {
            return currencyConversionService.latestRateToSgd(card.sourceCurrency())
                    .map(rate -> currencyConversionService.convertToSgd(
                            card.displayRawPrice(),
                            card.sourceCurrency(),
                            rate))
                    .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String exchangeRateNote(ExternalPricingProbeCardView card) {
        if (card.displayRawPrice() == null || card.sourceCurrency() == null) {
            return "No raw price or currency to convert.";
        }
        try {
            return currencyConversionService.latestRateToSgd(card.sourceCurrency())
                    .map(rate -> "SGD preview uses latest stored " + card.sourceCurrency() + " to SGD rate.")
                    .orElse("Missing " + card.sourceCurrency() + " to SGD exchange rate.");
        } catch (RuntimeException ex) {
            return "Invalid source currency.";
        }
    }

    private String requireSearchOrId(String searchOrId) {
        if (searchOrId == null || searchOrId.isBlank()) {
            throw new PricingProviderProbeException("Card id or search text is required.");
        }
        return searchOrId.trim();
    }
}
