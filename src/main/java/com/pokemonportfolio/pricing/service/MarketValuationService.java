package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.repository.CardRepository;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.config.domain.PricingMatchClassification;
import com.pokemonportfolio.config.domain.VerificationStatus;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.provider.PricingProviderCardPrices;
import com.pokemonportfolio.pricing.provider.PricingProviderException;
import com.pokemonportfolio.pricing.provider.PricingProviderPrice;
import com.pokemonportfolio.pricing.provider.PricingProviderSkipReason;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProviderAdapter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketValuationService {

    private final CardRepository cardRepository;
    private final OwnedItemService ownedItemService;
    private final PricingProviderService pricingProviderService;
    private final PriceSnapshotService priceSnapshotService;
    private final PricingProviderResultService pricingProviderResultService;
    private final ExchangeRateRefreshService exchangeRateRefreshService;

    public MarketValuationService(
            CardRepository cardRepository,
            OwnedItemService ownedItemService,
            PricingProviderService pricingProviderService,
            PriceSnapshotService priceSnapshotService,
            PricingProviderResultService pricingProviderResultService,
            ExchangeRateRefreshService exchangeRateRefreshService) {
        this.cardRepository = cardRepository;
        this.ownedItemService = ownedItemService;
        this.pricingProviderService = pricingProviderService;
        this.priceSnapshotService = priceSnapshotService;
        this.pricingProviderResultService = pricingProviderResultService;
        this.exchangeRateRefreshService = exchangeRateRefreshService;
    }

    @Transactional
    public PriceSnapshot refreshCardPrice(Card card) {
        PricingProviderPrice price = pricingProviderService.fetchBestCardPrice(card);
        return priceSnapshotService.createSnapshot(card, price);
    }

    @Transactional
    public int refreshAllActiveCards() {
        return cardRepository.findAll().stream()
                .filter(Card::isActive)
                .map(card -> {
                    try {
                        return refreshCardPrice(card);
                    } catch (RuntimeException ex) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList()
                .size();
    }

    @Transactional
    public PriceRefreshSummaryView refreshRealPrices(AppUser owner) {
        if (!pricingProviderService.isProviderEnabled(PokemonApiPricingProviderAdapter.PROVIDER_NAME)) {
            return new PriceRefreshSummaryView(
                    false,
                    pricingProviderService.unavailableMessage(PokemonApiPricingProviderAdapter.PROVIDER_NAME),
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    List.of());
        }

        RefreshCounters counters = new RefreshCounters();
        List<PriceRefreshSkippedItemView> skippedItems = new ArrayList<>();
        for (OwnedItem item : ownedItemService.listItems(owner)) {
            if (item.getStatus() != OwnedItemStatus.ACTIVE) {
                counters.skippedDisposedItems++;
                skippedItems.add(skip(item, "Skipped because item status is " + item.getStatus().getLabel() + "."));
                continue;
            }
            if (item.isSealedProduct()) {
                counters.skippedSealedProducts++;
                skippedItems.add(skip(item, "Sealed product pricing is deferred for this slice."));
                continue;
            }
            counters.totalActiveCardsChecked++;
            Card card = item.getCard();
            if (card.getVerificationStatus() != VerificationStatus.VERIFIED
                    || card.getCatalogSource() != CatalogSource.POKEMON_TCG_API) {
                counters.skippedManualCustomCards++;
                skippedItems.add(skip(item, "Manual/custom unverified cards use manual price entry fallback."));
                continue;
            }
            try {
                PricingProviderCardPrices prices = fetchCardPricesWithExchangeRateRefresh(card, item);
                pricingProviderResultService.storeCardResults(card, item.getOwnedVariant(), prices.sourceResults());
                priceSnapshotService.createSnapshot(card, item.getOwnedVariant(), prices.rawPrice());
                counters.snapshotsCreated++;
                incrementCreated(counters, prices.rawPrice());
            } catch (PricingProviderException ex) {
                increment(counters, ex);
                skippedItems.add(skip(item, ex.getMessage()));
            } catch (RuntimeException ex) {
                counters.providerErrors++;
                skippedItems.add(skip(item, "Pokemon API provider failed for this item."));
            }
        }

        return new PriceRefreshSummaryView(
                true,
                "Pokemon API refresh completed.",
                counters.totalActiveCardsChecked,
                counters.snapshotsCreated,
                counters.exactVariantSnapshotsCreated,
                counters.genericFallbackSnapshotsCreated,
                counters.tcgPlayerUsdSnapshotsCreated,
                counters.cardmarketEurSnapshotsCreated,
                counters.skippedManualCustomCards,
                counters.skippedDisposedItems,
                counters.skippedSealedProducts,
                counters.skippedNoProviderMatch,
                counters.skippedNoMatchingVariantPrice,
                counters.skippedUnsafeVariant,
                counters.skippedNoProviderPrice,
                counters.skippedMissingExchangeRate,
                counters.skippedMissingUsdExchangeRate,
                counters.skippedMissingEurExchangeRate,
                counters.providerErrors,
                skippedItems);
    }

    private void incrementCreated(RefreshCounters counters, PricingProviderPrice rawPrice) {
        PricingMatchClassification classification = rawPrice.pricingMatchClassification().orElse(null);
        if (classification == PricingMatchClassification.EXACT_VARIANT_MATCH) {
            counters.exactVariantSnapshotsCreated++;
        } else if (classification == PricingMatchClassification.GENERIC_RAW_FALLBACK) {
            counters.genericFallbackSnapshotsCreated++;
        }
        if ("TCGPLAYER".equalsIgnoreCase(rawPrice.sourceMarket())
                && "USD".equalsIgnoreCase(rawPrice.sourceCurrency())) {
            counters.tcgPlayerUsdSnapshotsCreated++;
        }
        if ("CARDMARKET".equalsIgnoreCase(rawPrice.sourceMarket())
                && "EUR".equalsIgnoreCase(rawPrice.sourceCurrency())) {
            counters.cardmarketEurSnapshotsCreated++;
        }
    }

    private PricingProviderCardPrices fetchCardPricesWithExchangeRateRefresh(Card card, OwnedItem item) {
        try {
            return pricingProviderService.fetchCardPrices(
                    PokemonApiPricingProviderAdapter.PROVIDER_NAME,
                    card,
                    item.getOwnedVariant());
        } catch (PricingProviderException ex) {
            if (ex.getSkipReason() != PricingProviderSkipReason.MISSING_EXCHANGE_RATE
                    || ex.getSourceCurrency() == null
                    || ex.getSourceCurrency().isBlank()) {
                throw ex;
            }
            try {
                exchangeRateRefreshService.ensureRateToSgd(ex.getSourceCurrency());
            } catch (ExchangeRateRefreshException refreshEx) {
                throw new PricingProviderException(
                        PricingProviderSkipReason.MISSING_EXCHANGE_RATE,
                        "Could not fetch " + ex.getSourceCurrency() + " to SGD exchange rate.",
                        ex.getSourceCurrency());
            }
            return pricingProviderService.fetchCardPrices(
                    PokemonApiPricingProviderAdapter.PROVIDER_NAME,
                    card,
                    item.getOwnedVariant());
        }
    }

    private void increment(RefreshCounters counters, PricingProviderException ex) {
        PricingProviderSkipReason reason = ex.getSkipReason();
        switch (reason) {
            case NO_PROVIDER_MATCH -> counters.skippedNoProviderMatch++;
            case NO_MATCHING_VARIANT_PRICE -> counters.skippedNoMatchingVariantPrice++;
            case UNSAFE_VARIANT_MISMATCH -> counters.skippedUnsafeVariant++;
            case NO_PRICE_AVAILABLE -> counters.skippedNoProviderPrice++;
            case MISSING_EXCHANGE_RATE -> {
                counters.skippedMissingExchangeRate++;
                if ("USD".equalsIgnoreCase(ex.getSourceCurrency())) {
                    counters.skippedMissingUsdExchangeRate++;
                }
                if ("EUR".equalsIgnoreCase(ex.getSourceCurrency())) {
                    counters.skippedMissingEurExchangeRate++;
                }
            }
            case PROVIDER_ERROR -> counters.providerErrors++;
        }
    }

    private PriceRefreshSkippedItemView skip(OwnedItem item, String reason) {
        return new PriceRefreshSkippedItemView(item.displayName(), reason);
    }

    private static class RefreshCounters {
        private int totalActiveCardsChecked;
        private int snapshotsCreated;
        private int exactVariantSnapshotsCreated;
        private int genericFallbackSnapshotsCreated;
        private int tcgPlayerUsdSnapshotsCreated;
        private int cardmarketEurSnapshotsCreated;
        private int skippedManualCustomCards;
        private int skippedDisposedItems;
        private int skippedSealedProducts;
        private int skippedNoProviderMatch;
        private int skippedNoMatchingVariantPrice;
        private int skippedUnsafeVariant;
        private int skippedNoProviderPrice;
        private int skippedMissingExchangeRate;
        private int skippedMissingUsdExchangeRate;
        private int skippedMissingEurExchangeRate;
        private int providerErrors;
    }
}
