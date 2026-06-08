package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.provider.PricingProviderPrice;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceSnapshotService {

    private final PriceSnapshotRepository priceSnapshotRepository;

    public PriceSnapshotService(PriceSnapshotRepository priceSnapshotRepository) {
        this.priceSnapshotRepository = priceSnapshotRepository;
    }

    @Transactional
    public PriceSnapshot createSnapshot(Card card, PricingProviderPrice price) {
        return createSnapshot(card, null, price);
    }

    @Transactional
    public PriceSnapshot createSnapshot(Card card, CardVariant cardVariant, PricingProviderPrice price) {
        PriceSnapshot snapshot = new PriceSnapshot(
                card,
                cardVariant,
                price.providerName(),
                price.sourceMarket(),
                MoneyCalculationSupport.money(price.sourcePrice()),
                price.sourceCurrency(),
                price.exchangeRateUsed(),
                MoneyCalculationSupport.money(price.marketPriceSgd()),
                price.confidenceRating(),
                price.explanation(),
                OffsetDateTime.now(),
                price.sourceUrl(),
                price.sourceUpdatedAt(),
                price.providerMetadata());
        return priceSnapshotRepository.save(snapshot);
    }

    @Transactional
    public PriceSnapshot createSnapshot(SealedProduct sealedProduct, PricingProviderPrice price) {
        PriceSnapshot snapshot = new PriceSnapshot(
                sealedProduct,
                price.providerName(),
                MoneyCalculationSupport.money(price.sourcePrice()),
                price.sourceCurrency(),
                price.exchangeRateUsed(),
                MoneyCalculationSupport.money(price.marketPriceSgd()),
                price.confidenceRating(),
                price.explanation(),
                OffsetDateTime.now());
        return priceSnapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public Optional<PriceSnapshot> latestForCard(Long cardId) {
        return priceSnapshotRepository.findTopByCardIdOrderByCalculatedAtDescIdDesc(cardId);
    }

    @Transactional(readOnly = true)
    public Optional<PriceSnapshot> latestForCardVariant(Long cardId, CardVariant cardVariant) {
        if (cardVariant == null) {
            return priceSnapshotRepository.findTopByCardIdAndCardVariantIsNullOrderByCalculatedAtDescIdDesc(cardId);
        }
        return priceSnapshotRepository
                .findTopByCardIdAndCardVariantOrderByCalculatedAtDescIdDesc(cardId, cardVariant)
                .or(() -> priceSnapshotRepository.findTopByCardIdAndCardVariantIsNullOrderByCalculatedAtDescIdDesc(cardId));
    }

    @Transactional(readOnly = true)
    public Optional<PriceSnapshot> latestForSealedProduct(Long sealedProductId) {
        return priceSnapshotRepository.findTopBySealedProductIdOrderByCalculatedAtDescIdDesc(sealedProductId);
    }
}
