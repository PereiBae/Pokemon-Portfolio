package com.pokemonportfolio.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pokemonportfolio.alerts.service.PriceAlertService;
import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.OfficialCardSearchResult;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.PricingMatchClassification;
import com.pokemonportfolio.config.domain.PricingResultType;
import com.pokemonportfolio.grading.service.GradingAnalyzerService;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.service.OwnedItemDisposalForm;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioDisposalService;
import com.pokemonportfolio.portfolio.service.PortfolioValuationService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.provider.ExchangeRateProvider;
import com.pokemonportfolio.pricing.provider.ExchangeRateProviderException;
import com.pokemonportfolio.pricing.provider.ExchangeRateQuote;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingCardView;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProvider;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPsaPriceView;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiRawPriceView;
import com.pokemonportfolio.pricing.repository.ExchangeRateSnapshotRepository;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import com.pokemonportfolio.pricing.repository.PricingProviderResultRepository;
import com.pokemonportfolio.trade.service.TradeAnalyzerService;
import com.pokemonportfolio.trade.service.TradeCreateForm;
import com.pokemonportfolio.trade.service.TradeOutgoingItemForm;
import com.pokemonportfolio.trade.service.TradeTransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "app.pricing.providers.pokemon-api.enabled=true",
        "app.pricing.providers.pokemon-api.rapid-api-key=test-key",
        "app.pricing.providers.pokemon-api.base-url=https://example.test/pokemon-api"
})
@ActiveProfiles("test")
@Transactional
class PokemonApiRealPriceRefreshServiceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private PortfolioDisposalService disposalService;

    @Autowired
    private CurrencyConversionService currencyConversionService;

    @Autowired
    private ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;

    @Autowired
    private MarketValuationService marketValuationService;

    @Autowired
    private ManualPriceEntryService manualPriceEntryService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private PricingProviderResultRepository pricingProviderResultRepository;

    @Autowired
    private PortfolioValuationService portfolioValuationService;

    @Autowired
    private PriceAlertService priceAlertService;

    @Autowired
    private TradeTransactionService tradeTransactionService;

    @Autowired
    private TradeAnalyzerService tradeAnalyzerService;

    @Autowired
    private GradingAnalyzerService gradingAnalyzerService;

    @Autowired
    private com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProperties pokemonApiProperties;

    @MockBean
    private PokemonApiPricingProvider pokemonApiPricingProvider;

    @MockBean
    private ExchangeRateProvider exchangeRateProvider;

    @Test
    void activeVerifiedCardCreatesAppendOnlyPokemonApiSnapshotAndProviderResults() {
        AppUser owner = owner();
        recordUsdRate();
        Card card = verifiedCard("Giratina VSTAR", "Crown Zenith", "GG69", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.STANDARD, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Giratina VSTAR Crown Zenith GG69"))
                .thenReturn(Optional.of(providerCard(
                        "Giratina VSTAR",
                        "Crown Zenith",
                        "GG69",
                        "146.69",
                        "163.71",
                        Map.of(CardVariant.STANDARD, rawPrice("normal", "146.69", "163.71")))));

        PriceRefreshSummaryView first = marketValuationService.refreshRealPrices(owner);
        PriceRefreshSummaryView second = marketValuationService.refreshRealPrices(owner);

        assertThat(first.snapshotsCreated()).isEqualTo(1);
        assertThat(first.exactVariantSnapshotsCreated()).isEqualTo(1);
        assertThat(first.genericFallbackSnapshotsCreated()).isZero();
        assertThat(first.tcgPlayerUsdSnapshotsCreated()).isEqualTo(1);
        assertThat(first.cardmarketEurSnapshotsCreated()).isZero();
        assertThat(second.snapshotsCreated()).isEqualTo(1);
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId()))
                .hasSize(2)
                .allSatisfy(snapshot -> {
                    assertThat(snapshot.getProviderName()).isEqualTo("POKEMON_API");
                    assertThat(snapshot.getSourceMarket()).isEqualTo("TCGPLAYER");
                    assertThat(snapshot.getSourceCurrency()).isEqualTo("USD");
                    assertThat(snapshot.getExchangeRateUsed()).isEqualByComparingTo("1.35000000");
                    assertThat(snapshot.getMarketPriceSgd()).isEqualByComparingTo("198.03");
                    assertThat(snapshot.getConfidenceRating()).isEqualTo(ConfidenceRating.MEDIUM);
                    assertThat(snapshot.getCardVariant()).isEqualTo(CardVariant.STANDARD);
                    assertThat(snapshot.pricingMatchClassification())
                            .contains(PricingMatchClassification.EXACT_VARIANT_MATCH);
                    assertThat(snapshot.getSourceUrl()).isEqualTo("https://example.test/pokemon-api/cards/3852");
                });
        assertThat(pricingProviderResultRepository.findByCardIdOrderByCapturedAtDescIdDesc(card.getId()))
                .extracting(result -> result.getResultType())
                .contains(PricingResultType.RAW_CARD, PricingResultType.PSA_8, PricingResultType.PSA_9, PricingResultType.PSA_10);
    }

    @Test
    void manualDisposedVariantAndMissingExchangeRateItemsAreReportedAsSkipped() {
        exchangeRateSnapshotRepository.deleteAll();
        AppUser owner = owner();
        Card manual = manualCard("Manual Refresh Skip");
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(manual.getId(), CardVariant.STANDARD, "50.00"));
        Card masterBall = verifiedCard("Variant Skip", "Crown Zenith", "GG70", List.of(CardVariant.STANDARD, CardVariant.MASTER_BALL));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(masterBall.getId(), CardVariant.MASTER_BALL, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Variant Skip Crown Zenith GG70"))
                .thenReturn(Optional.of(providerCard("Variant Skip", "Crown Zenith", "GG70", "20.00", null)));
        Card disposed = verifiedCard("Disposed Skip", "Crown Zenith", "GG71");
        OwnedItem disposedItem = ownedItemService.addCardToPortfolio(
                owner,
                ownedItemForm(disposed.getId(), CardVariant.STANDARD, "50.00"));
        disposalService.sellItem(owner, disposedItem.getId(), saleForm("75.00"));
        Card traded = verifiedCard("Traded Skip", "Crown Zenith", "GG75");
        OwnedItem tradedItem = ownedItemService.addCardToPortfolio(
                owner,
                ownedItemForm(traded.getId(), CardVariant.STANDARD, "50.00"));
        tradedItem.markTraded(OffsetDateTime.now());
        Card deleted = verifiedCard("Deleted Skip", "Crown Zenith", "GG76");
        OwnedItem deletedItem = ownedItemService.addCardToPortfolio(
                owner,
                ownedItemForm(deleted.getId(), CardVariant.STANDARD, "50.00"));
        deletedItem.markDeleted(OffsetDateTime.now());
        Card missingFx = verifiedCard("Missing FX", "Crown Zenith", "GG72", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(missingFx.getId(), CardVariant.STANDARD, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Missing FX Crown Zenith GG72"))
                .thenReturn(Optional.of(providerCard("Missing FX", "Crown Zenith", "GG72", "20.00", null)));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.totalActiveCardsChecked()).isEqualTo(3);
        assertThat(summary.snapshotsCreated()).isZero();
        assertThat(summary.skippedManualCustomCards()).isEqualTo(1);
        assertThat(summary.skippedDisposedItems()).isEqualTo(3);
        assertThat(summary.skippedUnsafeVariant()).isEqualTo(1);
        assertThat(summary.skippedMissingExchangeRate()).isEqualTo(1);
        assertThat(summary.skippedItems()).extracting(PriceRefreshSkippedItemView::reason)
                .anyMatch(reason -> reason.contains("Manual/custom"))
                .anyMatch(reason -> reason.contains("variant requires exact pricing"))
                .anyMatch(reason -> reason.contains("Sold"))
                .anyMatch(reason -> reason.contains("Traded"))
                .anyMatch(reason -> reason.contains("Deleted"))
                .anyMatch(reason -> reason.contains("Could not fetch USD to SGD exchange rate"));
    }

    @Test
    void exactHolofoilProviderPriceCreatesMediumConfidenceSnapshot() {
        AppUser owner = owner();
        recordUsdRate();
        Card card = verifiedCard("Exact Holo", "Crown Zenith", "GG80", List.of(CardVariant.STANDARD, CardVariant.HOLO));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.HOLO, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Exact Holo Crown Zenith GG80"))
                .thenReturn(Optional.of(providerCard(
                        "Exact Holo",
                        "Crown Zenith",
                        "GG80",
                        "20.00",
                        null,
                        Map.of(CardVariant.HOLO, rawPrice("holofoil", "146.69", null)))));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.snapshotsCreated()).isEqualTo(1);
        assertThat(summary.exactVariantSnapshotsCreated()).isEqualTo(1);
        assertThat(summary.genericFallbackSnapshotsCreated()).isZero();
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId()))
                .singleElement()
                .satisfies(snapshot -> {
                    assertThat(snapshot.getCardVariant()).isEqualTo(CardVariant.HOLO);
                    assertThat(snapshot.getConfidenceRating()).isEqualTo(ConfidenceRating.MEDIUM);
                    assertThat(snapshot.getProviderMetadata())
                            .contains("match=EXACT_VARIANT_MATCH", "source_field=tcg_player.holofoil.market_price");
                });
    }

    @Test
    void cardmarketEurPriceCreatesSnapshotWhenEurRateExists() {
        AppUser owner = owner();
        recordEurRate();
        Card card = verifiedCard("Cardmarket Fallback", "Crown Zenith", "GG84", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.STANDARD, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Cardmarket Fallback Crown Zenith GG84"))
                .thenReturn(Optional.of(providerCardWithCardmarket(
                        "Cardmarket Fallback",
                        "Crown Zenith",
                        "GG84",
                        null,
                        null,
                        "EUR",
                        "120.00",
                        "125.00",
                        "110.00",
                        "118.00")));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.snapshotsCreated()).isEqualTo(1);
        assertThat(summary.tcgPlayerUsdSnapshotsCreated()).isZero();
        assertThat(summary.cardmarketEurSnapshotsCreated()).isEqualTo(1);
        assertThat(summary.genericFallbackSnapshotsCreated()).isEqualTo(1);
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId()))
                .singleElement()
                .satisfies(snapshot -> {
                    assertThat(snapshot.getProviderName()).isEqualTo("POKEMON_API");
                    assertThat(snapshot.getSourceMarket()).isEqualTo("CARDMARKET");
                    assertThat(snapshot.getSourceCurrency()).isEqualTo("EUR");
                    assertThat(snapshot.getSourcePrice()).isEqualByComparingTo("120.00");
                    assertThat(snapshot.getExchangeRateUsed()).isEqualByComparingTo("1.46000000");
                    assertThat(snapshot.getMarketPriceSgd()).isEqualByComparingTo("175.20");
                    assertThat(snapshot.getConfidenceRating()).isEqualTo(ConfidenceRating.LOW);
                    assertThat(snapshot.getProviderMetadata())
                            .contains("source_field=cardmarket.average_price", "match=GENERIC_RAW_FALLBACK");
                });
        assertThat(pricingProviderResultRepository.findByCardIdOrderByCapturedAtDescIdDesc(card.getId()))
                .filteredOn(result -> result.getResultType() == PricingResultType.RAW_CARD)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getSourceMarket()).isEqualTo("CARDMARKET");
                    assertThat(result.getSourceCurrency()).isEqualTo("EUR");
                });
    }

    @Test
    void refreshRealPricesAutoFetchesMissingEurToSgdRate() {
        exchangeRateSnapshotRepository.deleteAll();
        AppUser owner = owner();
        when(exchangeRateProvider.isEnabled()).thenReturn(true);
        when(exchangeRateProvider.latestRate("EUR", "SGD"))
                .thenReturn(exchangeQuote("EUR", "1.45670000"));
        Card card = verifiedCard("Auto EUR FX", "Crown Zenith", "GG87", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.STANDARD, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Auto EUR FX Crown Zenith GG87"))
                .thenReturn(Optional.of(providerCardWithCardmarket(
                        "Auto EUR FX",
                        "Crown Zenith",
                        "GG87",
                        null,
                        null,
                        "EUR",
                        "120.00",
                        null,
                        null,
                        null)));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.snapshotsCreated()).isEqualTo(1);
        assertThat(summary.cardmarketEurSnapshotsCreated()).isEqualTo(1);
        assertThat(summary.skippedMissingExchangeRate()).isZero();
        assertThat(exchangeRateSnapshotRepository
                .findTopBySourceCurrencyAndTargetCurrencyOrderByEffectiveAtDescIdDesc("EUR", "SGD"))
                .hasValueSatisfying(snapshot -> {
                    assertThat(snapshot.getRateSource()).isEqualTo("FRANKFURTER");
                    assertThat(snapshot.getExchangeRate()).isEqualByComparingTo("1.45670000");
                });
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId()))
                .singleElement()
                .satisfies(snapshot -> {
                    assertThat(snapshot.getSourceMarket()).isEqualTo("CARDMARKET");
                    assertThat(snapshot.getSourceCurrency()).isEqualTo("EUR");
                    assertThat(snapshot.getExchangeRateUsed()).isEqualByComparingTo("1.45670000");
                    assertThat(snapshot.getMarketPriceSgd()).isEqualByComparingTo("174.80");
                });
    }

    @Test
    void refreshRealPricesAutoFetchesMissingUsdToSgdRate() {
        exchangeRateSnapshotRepository.deleteAll();
        AppUser owner = owner();
        when(exchangeRateProvider.isEnabled()).thenReturn(true);
        when(exchangeRateProvider.latestRate("USD", "SGD"))
                .thenReturn(exchangeQuote("USD", "1.35000000"));
        Card card = verifiedCard("Auto USD FX", "Crown Zenith", "GG88", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.STANDARD, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Auto USD FX Crown Zenith GG88"))
                .thenReturn(Optional.of(providerCard(
                        "Auto USD FX",
                        "Crown Zenith",
                        "GG88",
                        "20.00",
                        null,
                        Map.of(CardVariant.STANDARD, rawPrice("normal", "20.00", null)))));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.snapshotsCreated()).isEqualTo(1);
        assertThat(summary.exactVariantSnapshotsCreated()).isEqualTo(1);
        assertThat(summary.tcgPlayerUsdSnapshotsCreated()).isEqualTo(1);
        assertThat(summary.skippedMissingExchangeRate()).isZero();
        assertThat(exchangeRateSnapshotRepository
                .findTopBySourceCurrencyAndTargetCurrencyOrderByEffectiveAtDescIdDesc("USD", "SGD"))
                .hasValueSatisfying(snapshot -> {
                    assertThat(snapshot.getRateSource()).isEqualTo("FRANKFURTER");
                    assertThat(snapshot.getExchangeRate()).isEqualByComparingTo("1.35000000");
                });
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId()))
                .singleElement()
                .satisfies(snapshot -> {
                    assertThat(snapshot.getSourceMarket()).isEqualTo("TCGPLAYER");
                    assertThat(snapshot.getSourceCurrency()).isEqualTo("USD");
                    assertThat(snapshot.getExchangeRateUsed()).isEqualByComparingTo("1.35000000");
                    assertThat(snapshot.getMarketPriceSgd()).isEqualByComparingTo("27.00");
                });
    }

    @Test
    void refreshRealPricesUsesManualRateWhenAutomaticProviderUnavailable() {
        AppUser owner = owner();
        recordEurRate();
        when(exchangeRateProvider.isEnabled()).thenReturn(true);
        Card card = verifiedCard("Manual EUR FX", "Crown Zenith", "GG89", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.STANDARD, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Manual EUR FX Crown Zenith GG89"))
                .thenReturn(Optional.of(providerCardWithCardmarket(
                        "Manual EUR FX",
                        "Crown Zenith",
                        "GG89",
                        null,
                        null,
                        "EUR",
                        "120.00",
                        null,
                        null,
                        null)));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.snapshotsCreated()).isEqualTo(1);
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId()))
                .singleElement()
                .satisfies(snapshot -> {
                    assertThat(snapshot.getSourceCurrency()).isEqualTo("EUR");
                    assertThat(snapshot.getExchangeRateUsed()).isEqualByComparingTo("1.46000000");
                    assertThat(snapshot.getMarketPriceSgd()).isEqualByComparingTo("175.20");
                });
        verify(exchangeRateProvider, never()).latestRate("EUR", "SGD");
    }

    @Test
    void providerFailureGivesFriendlyExchangeRateSkipReason() {
        exchangeRateSnapshotRepository.deleteAll();
        AppUser owner = owner();
        when(exchangeRateProvider.isEnabled()).thenReturn(true);
        when(exchangeRateProvider.latestRate("EUR", "SGD"))
                .thenThrow(new ExchangeRateProviderException("Frankfurter unavailable."));
        Card card = verifiedCard("Failed EUR FX", "Crown Zenith", "GG90", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.STANDARD, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Failed EUR FX Crown Zenith GG90"))
                .thenReturn(Optional.of(providerCardWithCardmarket(
                        "Failed EUR FX",
                        "Crown Zenith",
                        "GG90",
                        null,
                        null,
                        "EUR",
                        "120.00",
                        null,
                        null,
                        null)));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.snapshotsCreated()).isZero();
        assertThat(summary.skippedMissingExchangeRate()).isEqualTo(1);
        assertThat(summary.skippedMissingEurExchangeRate()).isEqualTo(1);
        assertThat(summary.skippedItems()).extracting(PriceRefreshSkippedItemView::reason)
                .contains("Could not fetch EUR to SGD exchange rate.");
    }

    @Test
    void missingEurRateIsCountedSeparately() {
        exchangeRateSnapshotRepository.deleteAll();
        AppUser owner = owner();
        Card card = verifiedCard("Missing EUR FX", "Crown Zenith", "GG85", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.STANDARD, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Missing EUR FX Crown Zenith GG85"))
                .thenReturn(Optional.of(providerCardWithCardmarket(
                        "Missing EUR FX",
                        "Crown Zenith",
                        "GG85",
                        null,
                        null,
                        "EUR",
                        "120.00",
                        null,
                        null,
                        null)));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.snapshotsCreated()).isZero();
        assertThat(summary.skippedMissingExchangeRate()).isEqualTo(1);
        assertThat(summary.skippedMissingUsdExchangeRate()).isZero();
        assertThat(summary.skippedMissingEurExchangeRate()).isEqualTo(1);
        assertThat(summary.skippedItems()).extracting(PriceRefreshSkippedItemView::reason)
                .contains("Could not fetch EUR to SGD exchange rate.");
    }

    @Test
    void noSupportedRawPriceIsReportedWithoutSnapshot() {
        AppUser owner = owner();
        recordUsdRate();
        recordEurRate();
        Card card = verifiedCard("No Supported Raw", "Crown Zenith", "GG86", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.STANDARD, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("No Supported Raw Crown Zenith GG86"))
                .thenReturn(Optional.of(providerCardWithCardmarket(
                        "No Supported Raw",
                        "Crown Zenith",
                        "GG86",
                        null,
                        null,
                        "EUR",
                        null,
                        null,
                        null,
                        null)));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.snapshotsCreated()).isZero();
        assertThat(summary.skippedNoProviderPrice()).isEqualTo(1);
        assertThat(summary.skippedItems()).extracting(PriceRefreshSkippedItemView::reason)
                .contains("No supported raw price available.");
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId())).isEmpty();
    }

    @Test
    void genericRawFallbackCreatesLowConfidenceSnapshotsForCompatibleVariantsAndManualOverrideStillWorks() {
        AppUser owner = owner();
        recordUsdRate();
        Card holo = verifiedCard("Generic Holo", "Crown Zenith", "GG81", List.of(CardVariant.STANDARD, CardVariant.HOLO));
        OwnedItem holoItem = ownedItemService.addCardToPortfolio(owner, ownedItemForm(holo.getId(), CardVariant.HOLO, "50.00"));
        Card reverse = verifiedCard("Generic Reverse", "Crown Zenith", "GG82", List.of(CardVariant.STANDARD, CardVariant.REVERSE_HOLO));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(reverse.getId(), CardVariant.REVERSE_HOLO, "50.00"));
        Card standard = verifiedCard("Generic Standard", "Crown Zenith", "GG83", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(standard.getId(), CardVariant.STANDARD, "50.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Generic Holo Crown Zenith GG81"))
                .thenReturn(Optional.of(providerCard("Generic Holo", "Crown Zenith", "GG81", "100.00", null)));
        when(pokemonApiPricingProvider.searchFirstCard("Generic Reverse Crown Zenith GG82"))
                .thenReturn(Optional.of(providerCard("Generic Reverse", "Crown Zenith", "GG82", "80.00", null)));
        when(pokemonApiPricingProvider.searchFirstCard("Generic Standard Crown Zenith GG83"))
                .thenReturn(Optional.of(providerCard("Generic Standard", "Crown Zenith", "GG83", "60.00", null)));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.snapshotsCreated()).isEqualTo(3);
        assertThat(summary.exactVariantSnapshotsCreated()).isZero();
        assertThat(summary.genericFallbackSnapshotsCreated()).isEqualTo(3);
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(holo.getId()))
                .singleElement()
                .satisfies(snapshot -> {
                    assertThat(snapshot.getConfidenceRating()).isEqualTo(ConfidenceRating.LOW);
                    assertThat(snapshot.getCardVariant()).isEqualTo(CardVariant.HOLO);
                    assertThat(snapshot.getExplanation())
                            .isEqualTo("Generic raw price used; provider did not supply variant-specific pricing.");
                    assertThat(snapshot.pricingMatchClassification())
                            .contains(PricingMatchClassification.GENERIC_RAW_FALLBACK);
                });
        assertThat(portfolioValuationService.calculateCurrentValue(owner).items())
                .filteredOn(item -> item.ownedItemId().equals(holoItem.getId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.confidenceRating()).isEqualTo(ConfidenceRating.LOW);
                    assertThat(item.hasPricingWarning()).isTrue();
                    assertThat(item.pricingWarningLabel()).isEqualTo("Generic raw fallback");
                    assertThat(item.pricingWarningDetail()).isEqualTo("Not variant-specific");
                });

        manualPriceEntryService.createManualSnapshot(owner, manualFormForOwnedItem(holoItem.getId()));

        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(holo.getId()))
                .hasSize(2)
                .first()
                .satisfies(snapshot -> {
                    assertThat(snapshot.getProviderName()).isEqualTo("MANUAL");
                    assertThat(snapshot.getCardVariant()).isEqualTo(CardVariant.HOLO);
                    assertThat(snapshot.getMarketPriceSgd()).isEqualByComparingTo("175.50");
                });
        assertThat(portfolioValuationService.calculateCurrentValue(owner).items())
                .filteredOn(item -> item.ownedItemId().equals(holoItem.getId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.marketValueSgd()).isEqualByComparingTo("175.50");
                    assertThat(item.hasPricingWarning()).isFalse();
                });
    }

    @Test
    void genericRawFallbackSkipsUnsafeFirstEditionVariant() {
        AppUser owner = owner();
        recordUsdRate();
        Card card = verifiedCard(
                "Unsafe First Edition",
                "Base Set",
                "4",
                List.of(CardVariant.STANDARD, CardVariant.FIRST_EDITION_HOLO));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.FIRST_EDITION_HOLO, "500.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Unsafe First Edition Base Set 4"))
                .thenReturn(Optional.of(providerCard("Unsafe First Edition", "Base Set", "4", "1000.00", null)));

        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);

        assertThat(summary.snapshotsCreated()).isZero();
        assertThat(summary.skippedUnsafeVariant()).isEqualTo(1);
        assertThat(summary.skippedItems()).extracting(PriceRefreshSkippedItemView::reason)
                .contains("Generic raw price exists, but variant requires exact pricing.");
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId())).isEmpty();
    }

    @Test
    void dashboardAlertsTradeAndGradingUseLatestPokemonApiSnapshots() {
        AppUser owner = owner();
        recordUsdRate();
        Card card = verifiedCard("Portfolio Giratina", "Crown Zenith", "GG73", List.of(CardVariant.STANDARD));
        OwnedItem item = ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.STANDARD, "80.00"));
        when(pokemonApiPricingProvider.searchFirstCard("Portfolio Giratina Crown Zenith GG73"))
                .thenReturn(Optional.of(providerCard(
                        "Portfolio Giratina",
                        "Crown Zenith",
                        "GG73",
                        "146.69",
                        null,
                        Map.of(CardVariant.STANDARD, rawPrice("normal", "146.69", null)))));

        marketValuationService.refreshRealPrices(owner);

        var dashboard = portfolioValuationService.calculateCurrentValue(owner);
        assertThat(dashboard.totalValueSgd()).isEqualByComparingTo("198.03");
        assertThat(dashboard.items().getFirst().confidenceRating()).isEqualTo(ConfidenceRating.MEDIUM);

        var alerts = priceAlertService.checkAlerts(owner);
        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().getCurrentMarketValueSgd()).isEqualByComparingTo("198.03");

        var trade = tradeTransactionService.createDraft(owner, tradeForm());
        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(item.getId()));
        tradeAnalyzerService.analyze(owner, trade.getId());
        var tradeView = tradeTransactionService.detailView(owner, trade.getId());
        assertThat(tradeView.outgoingItems().getFirst().marketValueSgd()).isEqualByComparingTo("198.03");

        var gradingForm = gradingAnalyzerService.formFor(owner, item.getId());
        assertThat(gradingForm.getRawValueSgd()).isEqualByComparingTo("198.03");
        assertThat(gradingForm.getPsa8ValueSgd()).isEqualByComparingTo("810.00");
        assertThat(gradingForm.getPsa9ValueSgd()).isEqualByComparingTo("1620.00");
        assertThat(gradingForm.getPsa10ValueSgd()).isEqualByComparingTo("3970.35");
    }

    @Test
    void providerDisabledSummaryDoesNotCreateSnapshots() {
        AppUser owner = owner();
        Card card = verifiedCard("Disabled Like", "Crown Zenith", "GG74", List.of(CardVariant.STANDARD));
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId(), CardVariant.STANDARD, "80.00"));

        boolean originalEnabled = pokemonApiProperties.isEnabled();
        pokemonApiProperties.setEnabled(false);
        PriceRefreshSummaryView summary;
        try {
            summary = marketValuationService.refreshRealPrices(owner);
        } finally {
            pokemonApiProperties.setEnabled(originalEnabled);
        }

        assertThat(summary.providerEnabled()).isFalse();
        assertThat(summary.providerMessage()).contains("POKEMON_API_PRICING_ENABLED=true");
        assertThat(summary.snapshotsCreated()).isZero();
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private void recordUsdRate() {
        currencyConversionService.recordManualRate("USD", new BigDecimal("1.35000000"), ConfidenceRating.MEDIUM);
    }

    private void recordEurRate() {
        currencyConversionService.recordManualRate("EUR", new BigDecimal("1.46000000"), ConfidenceRating.LOW);
    }

    private ExchangeRateQuote exchangeQuote(String sourceCurrency, String rate) {
        return new ExchangeRateQuote(
                sourceCurrency,
                "SGD",
                new BigDecimal(rate),
                "FRANKFURTER",
                OffsetDateTime.parse("2026-06-07T00:00:00Z"),
                OffsetDateTime.parse("2026-06-08T01:30:00Z"));
    }

    private Card verifiedCard(String name, String setName, String cardNumber) {
        return verifiedCard(name, setName, cardNumber, List.of(CardVariant.STANDARD, CardVariant.REVERSE_HOLO));
    }

    private Card verifiedCard(String name, String setName, String cardNumber, List<CardVariant> variants) {
        return cardService.importOfficialCard(new OfficialCardSearchResult(
                "ptcg-" + name + "-" + cardNumber + "-" + System.nanoTime(),
                name,
                "set-" + setName,
                setName,
                "Sword & Shield",
                LocalDate.of(2023, 1, 20),
                cardNumber,
                "Ultra Rare",
                "https://images.example/small.png",
                "https://images.example/large.png",
                "https://example.test/card",
                LanguageMarket.ENGLISH,
                CatalogSource.POKEMON_TCG_API,
                variants));
    }

    private Card manualCard(String name) {
        CardForm form = new CardForm();
        form.setName(name + " " + System.nanoTime());
        form.setSetName("Manual Refresh Test");
        form.setCardNumber("001");
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
    }

    private OwnedItemForm ownedItemForm(Long cardId, CardVariant variant, String purchasePrice) {
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(cardId);
        form.setVariant(variant);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        form.setPurchaseDate(LocalDate.of(2026, 6, 5));
        form.setGradedStatus(GradedStatus.UNGRADED);
        return form;
    }

    private OwnedItemDisposalForm saleForm(String saleValue) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 5));
        form.setSalePriceSgd(new BigDecimal(saleValue));
        return form;
    }

    private ManualPriceEntryForm manualFormForOwnedItem(Long ownedItemId) {
        ManualPriceEntryForm form = new ManualPriceEntryForm();
        form.setOwnedItemId(ownedItemId);
        form.setProviderName("manual");
        form.setSourcePrice(new BigDecimal("130.00"));
        form.setSourceCurrency("USD");
        form.setExchangeRateUsed(new BigDecimal("1.35000000"));
        form.setMarketPriceSgd(new BigDecimal("175.50"));
        form.setConfidenceRating(ConfidenceRating.LOW);
        form.setNotes("Manual override after generic fallback");
        return form;
    }

    private TradeCreateForm tradeForm() {
        TradeCreateForm form = new TradeCreateForm();
        form.setName("Pokemon API Snapshot Trade " + System.nanoTime());
        form.setOutgoingTradePercentage(new BigDecimal("100.00"));
        form.setIncomingTradePercentage(new BigDecimal("100.00"));
        return form;
    }

    private TradeOutgoingItemForm outgoingForm(Long ownedItemId) {
        TradeOutgoingItemForm form = new TradeOutgoingItemForm();
        form.setOwnedItemId(ownedItemId);
        return form;
    }

    private PokemonApiPricingCardView providerCard(
            String name,
            String setName,
            String cardNumber,
            String marketPrice,
            String midPrice) {
        return providerCard(name, setName, cardNumber, marketPrice, midPrice, Map.of());
    }

    private PokemonApiPricingCardView providerCard(
            String name,
            String setName,
            String cardNumber,
            String marketPrice,
            String midPrice,
            Map<CardVariant, PokemonApiRawPriceView> variantPrices) {
        return new PokemonApiPricingCardView(
                3852L,
                name,
                setName,
                cardNumber,
                "https://images.example/provider.png",
                "USD",
                marketPrice == null ? null : new BigDecimal(marketPrice),
                midPrice == null ? null : new BigDecimal(midPrice),
                variantPrices,
                new PokemonApiPsaPriceView(8, new BigDecimal("600.00"), 3, "USD"),
                new PokemonApiPsaPriceView(9, new BigDecimal("1200.00"), 4, "USD"),
                new PokemonApiPsaPriceView(10, new BigDecimal("2941.00"), 5, "USD"));
    }

    private PokemonApiPricingCardView providerCardWithCardmarket(
            String name,
            String setName,
            String cardNumber,
            String marketPrice,
            String midPrice,
            String cardmarketCurrency,
            String cardmarketAveragePrice,
            String cardmarketTrendPrice,
            String cardmarketLowPrice,
            String cardmarketAverageSellPrice) {
        return new PokemonApiPricingCardView(
                3852L,
                name,
                setName,
                cardNumber,
                "https://images.example/provider.png",
                "USD",
                marketPrice == null ? null : new BigDecimal(marketPrice),
                midPrice == null ? null : new BigDecimal(midPrice),
                cardmarketCurrency,
                cardmarketAveragePrice == null ? null : new BigDecimal(cardmarketAveragePrice),
                cardmarketTrendPrice == null ? null : new BigDecimal(cardmarketTrendPrice),
                cardmarketLowPrice == null ? null : new BigDecimal(cardmarketLowPrice),
                cardmarketAverageSellPrice == null ? null : new BigDecimal(cardmarketAverageSellPrice),
                Map.of(),
                new PokemonApiPsaPriceView(8, new BigDecimal("600.00"), 3, "USD"),
                new PokemonApiPsaPriceView(9, new BigDecimal("1200.00"), 4, "USD"),
                new PokemonApiPsaPriceView(10, new BigDecimal("2941.00"), 5, "USD"));
    }

    private PokemonApiRawPriceView rawPrice(String sourceKey, String marketPrice, String midPrice) {
        return new PokemonApiRawPriceView(
                sourceKey,
                "USD",
                marketPrice == null ? null : new BigDecimal(marketPrice),
                midPrice == null ? null : new BigDecimal(midPrice));
    }
}
