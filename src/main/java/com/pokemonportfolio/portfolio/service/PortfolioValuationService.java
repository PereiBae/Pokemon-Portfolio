package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.PricingMatchClassification;
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
    private final PortfolioDisposalService disposalService;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final PortfolioValuationSnapshotRepository snapshotRepository;

    public PortfolioValuationService(
            OwnedItemService ownedItemService,
            PortfolioDisposalService disposalService,
            PriceSnapshotRepository priceSnapshotRepository,
            PortfolioValuationSnapshotRepository snapshotRepository) {
        this.ownedItemService = ownedItemService;
        this.disposalService = disposalService;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public PortfolioDashboardView calculateCurrentValue(AppUser owner) {
        List<OwnedItem> items = ownedItemService.listActiveItems(owner);
        List<PortfolioItemView> itemViews = items.stream().map(this::toItemView).toList();
        List<PortfolioItemView> pricedItemViews = itemViews.stream()
                .filter(PortfolioItemView::hasMarketValue)
                .toList();

        BigDecimal totalValue = pricedItemViews.stream()
                .map(PortfolioItemView::marketValueSgd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = pricedItemViews.stream()
                .map(PortfolioItemView::purchasePriceSgd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gainLoss = pricedItemViews.stream()
                .map(PortfolioItemView::gainLossSgd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int lowConfidenceCount = (int) pricedItemViews.stream()
                .filter(item -> item.confidenceRating() == ConfidenceRating.LOW)
                .count();
        PortfolioDisposalSummary realized = disposalService.realizedSummary(owner);
        BigDecimal totalPerformance = MoneyCalculationSupport.money(gainLoss.add(realized.realizedGainLossSgd()));
        BigDecimal totalPerformanceBasis = totalCost.add(realized.realizedCostBasisSgd());

        return new PortfolioDashboardView(
                MoneyCalculationSupport.money(totalValue),
                MoneyCalculationSupport.money(totalCost),
                MoneyCalculationSupport.money(gainLoss),
                MoneyCalculationSupport.percent(gainLoss, totalCost),
                itemViews.size(),
                lowConfidenceCount,
                itemViews,
                realized.realizedGainLossSgd(),
                realized.realizedGainLossPercent(),
                realized.realizedCostBasisSgd(),
                totalPerformance,
                MoneyCalculationSupport.percent(totalPerformance, totalPerformanceBasis),
                0,
                List.of());
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
        Optional<PriceSnapshot> latest = latestPrice(ownedItem);
        BigDecimal marketValue = latest
                .map(PriceSnapshot::getMarketPriceSgd)
                .map(MoneyCalculationSupport::money)
                .orElse(null);
        ConfidenceRating confidence = latest.map(PriceSnapshot::getConfidenceRating).orElse(null);
        PricingMatchClassification matchClassification = latest
                .flatMap(PriceSnapshot::pricingMatchClassification)
                .orElse(null);
        BigDecimal gainLoss = marketValue == null
                ? null
                : marketValue.subtract(ownedItem.getPurchasePriceSgd());
        return new PortfolioItemView(
                ownedItem.getId(),
                ownedItem.getAssetType().getLabel(),
                ownedItem.assetName(),
                ownedItem.assetName(),
                ownedItem.setName(),
                ownedItem.assetNumber(),
                ownedItem.variantOrTypeLabel(),
                ownedItem.verificationStatusLabel(),
                ownedItem.imageSmallUrl(),
                ownedItem.conditionLabel(),
                ownedItem.getPurchasePriceSgd(),
                marketValue,
                gainLoss == null ? null : MoneyCalculationSupport.money(gainLoss),
                confidence,
                latest.map(PriceSnapshot::getProviderName).orElse(""),
                latest.map(PriceSnapshot::getSourceMarket).orElse(""),
                latest.map(PriceSnapshot::getSourceCurrency).orElse(""),
                matchClassification == null ? "" : matchClassification.getLabel(),
                matchClassification == PricingMatchClassification.GENERIC_RAW_FALLBACK);
    }

    private Optional<PriceSnapshot> latestPrice(OwnedItem ownedItem) {
        if (ownedItem.isSealedProduct()) {
            return priceSnapshotRepository
                    .findTopBySealedProductIdOrderByCalculatedAtDescIdDesc(ownedItem.getSealedProduct().getId());
        }
        return priceSnapshotRepository
                .findTopByCardIdAndCardVariantOrderByCalculatedAtDescIdDesc(
                        ownedItem.getCard().getId(),
                        ownedItem.getOwnedVariant())
                .or(() -> priceSnapshotRepository
                        .findTopByCardIdAndCardVariantIsNullOrderByCalculatedAtDescIdDesc(
                                ownedItem.getCard().getId()));
    }
}
