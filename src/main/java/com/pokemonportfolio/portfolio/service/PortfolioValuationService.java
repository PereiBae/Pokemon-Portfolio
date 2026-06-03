package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.entity.PortfolioValuationSnapshot;
import com.pokemonportfolio.portfolio.repository.PortfolioValuationSnapshotRepository;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioValuationService {

    private final OwnedItemService ownedItemService;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final PortfolioValuationSnapshotRepository snapshotRepository;

    public PortfolioValuationService(
            OwnedItemService ownedItemService,
            PriceSnapshotRepository priceSnapshotRepository,
            PortfolioValuationSnapshotRepository snapshotRepository) {
        this.ownedItemService = ownedItemService;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public PortfolioDashboardView calculateCurrentValue(AppUser owner) {
        List<OwnedItem> items = ownedItemService.listActiveItems(owner);
        List<PortfolioItemView> itemViews = items.stream().map(this::toItemView).toList();

        BigDecimal totalValue = itemViews.stream()
                .map(PortfolioItemView::marketValueSgd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = itemViews.stream()
                .map(PortfolioItemView::purchasePriceSgd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gainLoss = totalValue.subtract(totalCost);
        int lowConfidenceCount = (int) itemViews.stream()
                .filter(item -> item.confidenceRating() == ConfidenceRating.LOW)
                .count();

        return new PortfolioDashboardView(
                MoneyCalculationSupport.money(totalValue),
                MoneyCalculationSupport.money(totalCost),
                MoneyCalculationSupport.money(gainLoss),
                MoneyCalculationSupport.percent(gainLoss, totalCost),
                itemViews.size(),
                lowConfidenceCount,
                itemViews);
    }

    @Transactional
    public PortfolioValuationSnapshot createSnapshot(AppUser owner) {
        PortfolioDashboardView view = calculateCurrentValue(owner);
        PortfolioValuationSnapshot snapshot = new PortfolioValuationSnapshot(
                owner,
                view.totalValueSgd(),
                view.totalCostBasisSgd(),
                view.unrealizedGainLossSgd(),
                view.unrealizedGainLossPercent(),
                view.itemCount(),
                view.lowConfidenceCount(),
                LocalDate.now(),
                OffsetDateTime.now(),
                "Portfolio valuation calculated from latest append-only card price snapshots.");
        return snapshotRepository.save(snapshot);
    }

    private PortfolioItemView toItemView(OwnedItem ownedItem) {
        Optional<PriceSnapshot> latest = priceSnapshotRepository
                .findTopByCardIdOrderByCalculatedAtDescIdDesc(ownedItem.getCard().getId());
        BigDecimal marketValue = latest.map(PriceSnapshot::getMarketPriceSgd).orElse(BigDecimal.ZERO);
        ConfidenceRating confidence = latest.map(PriceSnapshot::getConfidenceRating).orElse(ConfidenceRating.LOW);
        BigDecimal gainLoss = marketValue.subtract(ownedItem.getPurchasePriceSgd());
        return new PortfolioItemView(
                ownedItem.getId(),
                ownedItem.getCard().getName(),
                ownedItem.getCard().getPokemonSet().getName(),
                ownedItem.getCard().getCardNumber(),
                ownedItem.getCondition().getLabel(),
                ownedItem.getPurchasePriceSgd(),
                MoneyCalculationSupport.money(marketValue),
                MoneyCalculationSupport.money(gainLoss),
                confidence);
    }
}

