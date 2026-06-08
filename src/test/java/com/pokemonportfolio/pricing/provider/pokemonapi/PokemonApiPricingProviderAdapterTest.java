package com.pokemonportfolio.pricing.provider.pokemonapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.PokemonSet;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.PricingMatchClassification;
import com.pokemonportfolio.config.domain.PricingResultType;
import com.pokemonportfolio.pricing.entity.ExchangeRateSnapshot;
import com.pokemonportfolio.pricing.provider.PricingProviderException;
import com.pokemonportfolio.pricing.provider.PricingProviderSkipReason;
import com.pokemonportfolio.pricing.repository.ExchangeRateSnapshotRepository;
import com.pokemonportfolio.pricing.service.CurrencyConversionService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PokemonApiPricingProviderAdapterTest {

    @Test
    void mapsRawTcgPlayerMarketPriceAndPsaResultsToInternalValues() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                new BigDecimal("146.69"),
                new BigDecimal("163.71"),
                Map.of(CardVariant.STANDARD, new PokemonApiRawPriceView(
                        "normal",
                        "USD",
                        new BigDecimal("146.69"),
                        new BigDecimal("163.71"))))));
        PokemonApiPricingProviderAdapter adapter = adapter(provider, Optional.of(exchangeRate("USD", "1.35000000")));

        var prices = adapter.fetchCardPrices(verifiedCard(), CardVariant.STANDARD);

        assertThat(prices.rawPrice().providerName()).isEqualTo("POKEMON_API");
        assertThat(prices.rawPrice().sourceMarket()).isEqualTo("TCGPLAYER");
        assertThat(prices.rawPrice().sourceCurrency()).isEqualTo("USD");
        assertThat(prices.rawPrice().sourcePrice()).isEqualByComparingTo("146.69");
        assertThat(prices.rawPrice().exchangeRateUsed()).isEqualByComparingTo("1.35000000");
        assertThat(prices.rawPrice().marketPriceSgd()).isEqualByComparingTo("198.03");
        assertThat(prices.rawPrice().confidenceRating()).isEqualTo(ConfidenceRating.MEDIUM);
        assertThat(prices.rawPrice().pricingMatchClassification())
                .contains(PricingMatchClassification.EXACT_VARIANT_MATCH);
        assertThat(prices.rawPrice().sourceUrl()).isEqualTo("https://example.test/pokemon-api/cards/3852");
        assertThat(prices.sourceResults()).extracting(result -> result.resultType())
                .containsExactly(PricingResultType.RAW_CARD, PricingResultType.PSA_8, PricingResultType.PSA_9, PricingResultType.PSA_10);
        assertThat(prices.sourceResults()).filteredOn(result -> result.resultType() == PricingResultType.PSA_8)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.sourceMarket()).isEqualTo("EBAY_SOLD_GRADED");
                    assertThat(result.sourceCurrency()).isEqualTo("USD");
                    assertThat(result.sourcePrice()).isEqualByComparingTo("600.00");
                    assertThat(result.priceSgd()).isEqualByComparingTo("810.00");
                    assertThat(result.sampleSize()).isEqualTo(3);
                    assertThat(result.confidenceRating()).isEqualTo(ConfidenceRating.LOW);
                });
        assertThat(prices.sourceResults()).filteredOn(result -> result.resultType() == PricingResultType.PSA_10)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.sourcePrice()).isEqualByComparingTo("2941.00");
                    assertThat(result.sampleSize()).isEqualTo(5);
                    assertThat(result.confidenceRating()).isEqualTo(ConfidenceRating.MEDIUM);
                });
    }

    @Test
    void fallsBackToTcgPlayerMidPriceWithLowConfidenceWhenMarketMissing() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                null,
                new BigDecimal("163.71"),
                Map.of())));
        PokemonApiPricingProviderAdapter adapter = adapter(provider, Optional.of(exchangeRate("USD", "1.35000000")));

        var prices = adapter.fetchCardPrices(verifiedCard(), CardVariant.STANDARD);

        assertThat(prices.rawPrice().sourcePrice()).isEqualByComparingTo("163.71");
        assertThat(prices.rawPrice().marketPriceSgd()).isEqualByComparingTo("221.01");
        assertThat(prices.rawPrice().confidenceRating()).isEqualTo(ConfidenceRating.LOW);
        assertThat(prices.rawPrice().providerMetadata())
                .contains("tcg_player.mid_price", "match=GENERIC_RAW_FALLBACK");
        assertThat(prices.rawPrice().explanation())
                .isEqualTo("Generic raw price used; provider did not supply variant-specific pricing.");
    }

    @Test
    void fallbackOrderPrefersTcgPlayerMarketOverCardmarketPrice() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                new BigDecimal("146.69"),
                null,
                "EUR",
                new BigDecimal("120.00"),
                null,
                null,
                null,
                Map.of())));
        PokemonApiPricingProviderAdapter adapter = adapter(
                provider,
                List.of(exchangeRate("USD", "1.35000000"), exchangeRate("EUR", "1.46000000")));

        var prices = adapter.fetchCardPrices(verifiedCard(), CardVariant.STANDARD);

        assertThat(prices.rawPrice().sourceMarket()).isEqualTo("TCGPLAYER");
        assertThat(prices.rawPrice().sourceCurrency()).isEqualTo("USD");
        assertThat(prices.rawPrice().sourcePrice()).isEqualByComparingTo("146.69");
        assertThat(prices.rawPrice().providerMetadata()).contains("source_field=tcg_player.market_price");
    }

    @Test
    void cardmarketAveragePriceCreatesEurSnapshotInputWhenTcgPlayerMissing() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                null,
                null,
                "EUR",
                new BigDecimal("120.00"),
                new BigDecimal("125.00"),
                new BigDecimal("110.00"),
                new BigDecimal("118.00"),
                Map.of())));
        PokemonApiPricingProviderAdapter adapter = adapter(provider, Optional.of(exchangeRate("EUR", "1.46000000")));

        var prices = adapter.fetchCardPrices(verifiedCard(), CardVariant.STANDARD);

        assertThat(prices.rawPrice().sourceMarket()).isEqualTo("CARDMARKET");
        assertThat(prices.rawPrice().sourceCurrency()).isEqualTo("EUR");
        assertThat(prices.rawPrice().sourcePrice()).isEqualByComparingTo("120.00");
        assertThat(prices.rawPrice().marketPriceSgd()).isEqualByComparingTo("175.20");
        assertThat(prices.rawPrice().confidenceRating()).isEqualTo(ConfidenceRating.LOW);
        assertThat(prices.rawPrice().providerMetadata())
                .contains("source_field=cardmarket.average_price", "match=GENERIC_RAW_FALLBACK");
    }

    @Test
    void cardmarketTrendPriceIsUsedWhenAverageMissing() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                null,
                null,
                "EUR",
                null,
                new BigDecimal("125.00"),
                new BigDecimal("110.00"),
                new BigDecimal("118.00"),
                Map.of())));
        PokemonApiPricingProviderAdapter adapter = adapter(provider, Optional.of(exchangeRate("EUR", "1.46000000")));

        var prices = adapter.fetchCardPrices(verifiedCard(), CardVariant.STANDARD);

        assertThat(prices.rawPrice().sourceMarket()).isEqualTo("CARDMARKET");
        assertThat(prices.rawPrice().sourcePrice()).isEqualByComparingTo("125.00");
        assertThat(prices.rawPrice().providerMetadata()).contains("source_field=cardmarket.trend_price");
    }

    @Test
    void exactHolofoilProviderPriceCreatesMediumConfidenceSnapshotInput() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                new BigDecimal("100.00"),
                null,
                Map.of(CardVariant.HOLO, new PokemonApiRawPriceView(
                        "holofoil",
                        "USD",
                        new BigDecimal("146.69"),
                        null)))));
        PokemonApiPricingProviderAdapter adapter = adapter(provider, Optional.of(exchangeRate("USD", "1.35000000")));

        var prices = adapter.fetchCardPrices(verifiedCard(List.of(CardVariant.STANDARD, CardVariant.HOLO)), CardVariant.HOLO);

        assertThat(prices.rawPrice().sourcePrice()).isEqualByComparingTo("146.69");
        assertThat(prices.rawPrice().marketPriceSgd()).isEqualByComparingTo("198.03");
        assertThat(prices.rawPrice().confidenceRating()).isEqualTo(ConfidenceRating.MEDIUM);
        assertThat(prices.rawPrice().providerMetadata())
                .contains("source_field=tcg_player.holofoil.market_price", "match=EXACT_VARIANT_MATCH", "variant=HOLO");
    }

    @Test
    void genericRawPriceForCompatibleVariantsCreatesLowConfidenceFallback() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                new BigDecimal("146.69"),
                null,
                Map.of())));
        PokemonApiPricingProviderAdapter adapter = adapter(provider, Optional.of(exchangeRate("USD", "1.35000000")));

        var holo = adapter.fetchCardPrices(verifiedCard(List.of(CardVariant.STANDARD, CardVariant.HOLO)), CardVariant.HOLO);
        var reverse = adapter.fetchCardPrices(
                verifiedCard(List.of(CardVariant.STANDARD, CardVariant.REVERSE_HOLO)),
                CardVariant.REVERSE_HOLO);
        var standard = adapter.fetchCardPrices(verifiedCard(), CardVariant.STANDARD);

        assertThat(holo.rawPrice().confidenceRating()).isEqualTo(ConfidenceRating.LOW);
        assertThat(reverse.rawPrice().confidenceRating()).isEqualTo(ConfidenceRating.LOW);
        assertThat(standard.rawPrice().confidenceRating()).isEqualTo(ConfidenceRating.LOW);
        assertThat(reverse.rawPrice().providerMetadata())
                .contains("match=GENERIC_RAW_FALLBACK", "variant=REVERSE_HOLO");
        assertThat(reverse.rawPrice().explanation())
                .isEqualTo("Generic raw price used; provider did not supply variant-specific pricing.");
    }

    @Test
    void missingExchangeRateSkipsWithFriendlyReason() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                new BigDecimal("146.69"),
                null,
                Map.of())));
        PokemonApiPricingProviderAdapter adapter = adapter(provider, Optional.empty());

        assertThatThrownBy(() -> adapter.fetchCardPrices(verifiedCard(), CardVariant.STANDARD))
                .isInstanceOf(PricingProviderException.class)
                .satisfies(ex -> assertThat(((PricingProviderException) ex).getSkipReason())
                        .isEqualTo(PricingProviderSkipReason.MISSING_EXCHANGE_RATE))
                .hasMessage("Missing USD to SGD exchange rate.");
    }

    @Test
    void missingCardmarketExchangeRateSkipsWithFriendlyEurReason() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                null,
                null,
                "EUR",
                new BigDecimal("120.00"),
                null,
                null,
                null,
                Map.of())));
        PokemonApiPricingProviderAdapter adapter = adapter(provider, Optional.empty());

        assertThatThrownBy(() -> adapter.fetchCardPrices(verifiedCard(), CardVariant.STANDARD))
                .isInstanceOf(PricingProviderException.class)
                .satisfies(ex -> {
                    PricingProviderException providerException = (PricingProviderException) ex;
                    assertThat(providerException.getSkipReason()).isEqualTo(PricingProviderSkipReason.MISSING_EXCHANGE_RATE);
                    assertThat(providerException.getSourceCurrency()).isEqualTo("EUR");
                })
                .hasMessage("Missing EUR to SGD exchange rate.");
    }

    @Test
    void noSupportedRawPriceSkipsWithFriendlyReason() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of())));
        PokemonApiPricingProviderAdapter adapter = adapter(provider, Optional.of(exchangeRate("USD", "1.35000000")));

        assertThatThrownBy(() -> adapter.fetchCardPrices(verifiedCard(), CardVariant.STANDARD))
                .isInstanceOf(PricingProviderException.class)
                .satisfies(ex -> assertThat(((PricingProviderException) ex).getSkipReason())
                        .isEqualTo(PricingProviderSkipReason.NO_PRICE_AVAILABLE))
                .hasMessage("No supported raw price available.");
    }

    @Test
    void unsafeSpecialVariantsSkipWhenOnlyGenericRawPriceExists() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.searchFirstCard("Giratina VSTAR Crown Zenith GG69")).thenReturn(Optional.of(cardView(
                new BigDecimal("146.69"),
                null,
                Map.of())));
        PokemonApiPricingProviderAdapter adapter = adapter(provider, Optional.of(exchangeRate("USD", "1.35000000")));

        assertThatThrownBy(() -> adapter.fetchCardPrices(
                verifiedCard(List.of(CardVariant.STANDARD, CardVariant.MASTER_BALL)),
                CardVariant.MASTER_BALL))
                .isInstanceOf(PricingProviderException.class)
                .satisfies(ex -> assertThat(((PricingProviderException) ex).getSkipReason())
                        .isEqualTo(PricingProviderSkipReason.UNSAFE_VARIANT_MISMATCH))
                .hasMessage("Generic raw price exists, but variant requires exact pricing.");
        assertThatThrownBy(() -> adapter.fetchCardPrices(
                verifiedCard(List.of(CardVariant.STANDARD, CardVariant.FIRST_EDITION_HOLO)),
                CardVariant.FIRST_EDITION_HOLO))
                .isInstanceOf(PricingProviderException.class)
                .satisfies(ex -> assertThat(((PricingProviderException) ex).getSkipReason())
                        .isEqualTo(PricingProviderSkipReason.UNSAFE_VARIANT_MISMATCH));
    }

    private PokemonApiPricingProviderAdapter adapter(
            PokemonApiPricingProvider provider,
            Optional<ExchangeRateSnapshot> exchangeRate) {
        return adapter(provider, exchangeRate.stream().toList());
    }

    private PokemonApiPricingProviderAdapter adapter(
            PokemonApiPricingProvider provider,
            List<ExchangeRateSnapshot> exchangeRates) {
        PokemonApiPricingProperties properties = new PokemonApiPricingProperties();
        properties.setEnabled(true);
        properties.setRapidApiKey("test-key");
        properties.setBaseUrl("https://example.test/pokemon-api");
        ExchangeRateSnapshotRepository repository = mock(ExchangeRateSnapshotRepository.class);
        when(repository.findTopBySourceCurrencyAndTargetCurrencyOrderByEffectiveAtDescIdDesc(anyString(), eq("SGD")))
                .thenReturn(Optional.empty());
        for (ExchangeRateSnapshot exchangeRate : exchangeRates) {
            when(repository.findTopBySourceCurrencyAndTargetCurrencyOrderByEffectiveAtDescIdDesc(
                    exchangeRate.getSourceCurrency(),
                    exchangeRate.getTargetCurrency()))
                    .thenReturn(Optional.of(exchangeRate));
        }
        return new PokemonApiPricingProviderAdapter(
                provider,
                properties,
                new CurrencyConversionService(repository));
    }

    private ExchangeRateSnapshot exchangeRate(String sourceCurrency, String exchangeRate) {
        return new ExchangeRateSnapshot(
                sourceCurrency,
                "SGD",
                new BigDecimal(exchangeRate),
                "TEST",
                ConfidenceRating.MEDIUM,
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }

    private Card verifiedCard() {
        return verifiedCard(List.of(CardVariant.STANDARD));
    }

    private Card verifiedCard(List<CardVariant> availableVariants) {
        Card card = new Card(
                new PokemonSet("Crown Zenith", LanguageMarket.ENGLISH),
                "Giratina VSTAR",
                "GG69",
                LanguageMarket.ENGLISH,
                CardVariant.STANDARD);
        card.markVerified(
                CatalogSource.POKEMON_TCG_API,
                "swsh12pt5-GG69",
                "https://images.example/small.png",
                "https://images.example/large.png",
                "https://example.test/card",
                "Ultra Rare",
                availableVariants,
                OffsetDateTime.now());
        return card;
    }

    private PokemonApiPricingCardView cardView(
            BigDecimal marketPrice,
            BigDecimal midPrice,
            Map<CardVariant, PokemonApiRawPriceView> variantPrices) {
        return cardView(marketPrice, midPrice, null, null, null, null, null, variantPrices);
    }

    private PokemonApiPricingCardView cardView(
            BigDecimal marketPrice,
            BigDecimal midPrice,
            String cardmarketCurrency,
            BigDecimal cardmarketAveragePrice,
            BigDecimal cardmarketTrendPrice,
            BigDecimal cardmarketLowPrice,
            BigDecimal cardmarketAverageSellPrice,
            Map<CardVariant, PokemonApiRawPriceView> variantPrices) {
        return new PokemonApiPricingCardView(
                3852L,
                "Giratina VSTAR",
                "Crown Zenith",
                "GG69",
                "https://images.example/giratina.png",
                "USD",
                marketPrice,
                midPrice,
                cardmarketCurrency,
                cardmarketAveragePrice,
                cardmarketTrendPrice,
                cardmarketLowPrice,
                cardmarketAverageSellPrice,
                variantPrices,
                new PokemonApiPsaPriceView(8, new BigDecimal("600.00"), 3, "USD"),
                new PokemonApiPsaPriceView(9, new BigDecimal("1200.00"), 4, "USD"),
                new PokemonApiPsaPriceView(10, new BigDecimal("2941.00"), 5, "USD"));
    }
}
