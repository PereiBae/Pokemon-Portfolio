package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.repository.CardRepository;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.provider.PricingProviderPrice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketValuationService {

    private final CardRepository cardRepository;
    private final PricingProviderService pricingProviderService;
    private final PriceSnapshotService priceSnapshotService;

    public MarketValuationService(
            CardRepository cardRepository,
            PricingProviderService pricingProviderService,
            PriceSnapshotService priceSnapshotService) {
        this.cardRepository = cardRepository;
        this.pricingProviderService = pricingProviderService;
        this.priceSnapshotService = priceSnapshotService;
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
                .map(this::refreshCardPrice)
                .toList()
                .size();
    }
}

