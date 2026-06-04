package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.pricing.service.MarketValuationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioWorkflowService {

    private final OwnedItemService ownedItemService;
    private final MarketValuationService marketValuationService;
    private final PortfolioValuationService portfolioValuationService;

    public PortfolioWorkflowService(
            OwnedItemService ownedItemService,
            MarketValuationService marketValuationService,
            PortfolioValuationService portfolioValuationService) {
        this.ownedItemService = ownedItemService;
        this.marketValuationService = marketValuationService;
        this.portfolioValuationService = portfolioValuationService;
    }

    @Transactional
    public OwnedItem addCardToPortfolioAndSnapshot(AppUser owner, OwnedItemForm form) {
        OwnedItem ownedItem = ownedItemService.addCardToPortfolio(owner, form);
        marketValuationService.refreshCardPrice(ownedItem.getCard());
        portfolioValuationService.createSnapshot(owner);
        return ownedItem;
    }

    @Transactional
    public OwnedItem addSealedProductToPortfolioAndSnapshot(AppUser owner, OwnedItemForm form) {
        OwnedItem ownedItem = ownedItemService.addSealedProductToPortfolio(owner, form);
        portfolioValuationService.createSnapshot(owner);
        return ownedItem;
    }
}
