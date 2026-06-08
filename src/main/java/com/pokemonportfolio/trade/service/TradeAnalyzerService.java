package com.pokemonportfolio.trade.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.TradeFairnessResult;
import com.pokemonportfolio.config.domain.TradeSideType;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import com.pokemonportfolio.trade.entity.TradeTransaction;
import com.pokemonportfolio.trade.entity.TradeTransactionItem;
import com.pokemonportfolio.trade.entity.TradeTransactionSide;
import com.pokemonportfolio.trade.repository.TradeTransactionItemRepository;
import com.pokemonportfolio.trade.repository.TradeTransactionRepository;
import com.pokemonportfolio.trade.repository.TradeTransactionSideRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeAnalyzerService {

    private static final BigDecimal BALANCED_TOLERANCE_SGD = new BigDecimal("5.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final TradeTransactionRepository tradeTransactionRepository;
    private final TradeTransactionSideRepository tradeTransactionSideRepository;
    private final TradeTransactionItemRepository tradeTransactionItemRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;

    public TradeAnalyzerService(
            TradeTransactionRepository tradeTransactionRepository,
            TradeTransactionSideRepository tradeTransactionSideRepository,
            TradeTransactionItemRepository tradeTransactionItemRepository,
            PriceSnapshotRepository priceSnapshotRepository) {
        this.tradeTransactionRepository = tradeTransactionRepository;
        this.tradeTransactionSideRepository = tradeTransactionSideRepository;
        this.tradeTransactionItemRepository = tradeTransactionItemRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
    }

    @Transactional
    public TradeTransaction analyze(AppUser owner, Long tradeTransactionId) {
        TradeTransaction tradeTransaction = tradeTransactionRepository.findByIdAndOwner(tradeTransactionId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Trade transaction not found"));
        if (tradeTransaction.isExecuted()) {
            throw new IllegalArgumentException("Executed trades cannot be re-analysed");
        }
        if (tradeTransaction.isCancelled()) {
            throw new IllegalArgumentException("Cancelled trades cannot be analysed");
        }
        List<TradeTransactionSide> sides = tradeTransactionSideRepository.findByTradeTransaction(tradeTransaction);
        Map<TradeSideType, TradeTransactionSide> sideByType = sides.stream()
                .collect(Collectors.toMap(TradeTransactionSide::getSideType, Function.identity()));
        List<TradeTransactionItem> outgoingItems = tradeTransactionItemRepository
                .findByTradeTransactionAndSideTypeOrderByCreatedAtAscIdAsc(tradeTransaction, TradeSideType.OUTGOING);
        List<TradeTransactionItem> incomingItems = tradeTransactionItemRepository
                .findByTradeTransactionAndSideTypeOrderByCreatedAtAscIdAsc(tradeTransaction, TradeSideType.INCOMING);

        SideTotals outgoingTotals = calculateSide(outgoingItems, sideByType.get(TradeSideType.OUTGOING));
        SideTotals incomingTotals = calculateSide(incomingItems, sideByType.get(TradeSideType.INCOMING));

        BigDecimal netDifference = MoneyCalculationSupport.money(
                incomingTotals.adjustedValueSgd().subtract(outgoingTotals.adjustedValueSgd()));
        BigDecimal tradeImbalance = netDifference;
        tradeTransaction.updateAnalysis(
                outgoingTotals.marketValueSgd(),
                incomingTotals.marketValueSgd(),
                outgoingTotals.agreedValueSgd(),
                incomingTotals.agreedValueSgd(),
                outgoingTotals.adjustedValueSgd(),
                incomingTotals.adjustedValueSgd(),
                netDifference,
                tradeImbalance,
                fairness(netDifference),
                overallConfidence(outgoingItems, incomingItems),
                OffsetDateTime.now());
        return tradeTransactionRepository.save(tradeTransaction);
    }

    private SideTotals calculateSide(List<TradeTransactionItem> items, TradeTransactionSide side) {
        BigDecimal marketTotal = BigDecimal.ZERO;
        BigDecimal agreedTotal = BigDecimal.ZERO;
        BigDecimal adjustedTotal = BigDecimal.ZERO;
        for (TradeTransactionItem item : items) {
            PricePoint pricePoint = latestPrice(item);
            BigDecimal baseValue = baseValue(item, pricePoint);
            BigDecimal adjustedValue = MoneyCalculationSupport.money(baseValue
                    .multiply(side.getTradePercentage())
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
            item.updateCalculatedValues(
                    pricePoint.marketValueSgd(),
                    baseValue,
                    adjustedValue,
                    pricePoint.confidenceRating());
            marketTotal = marketTotal.add(pricePoint.marketValueSgd());
            agreedTotal = agreedTotal.add(baseValue);
            adjustedTotal = adjustedTotal.add(adjustedValue);
        }
        SideTotals totals = new SideTotals(
                MoneyCalculationSupport.money(marketTotal),
                MoneyCalculationSupport.money(agreedTotal),
                MoneyCalculationSupport.money(adjustedTotal));
        side.updateTotals(totals.marketValueSgd(), totals.agreedValueSgd(), totals.adjustedValueSgd());
        return totals;
    }

    private PricePoint latestPrice(TradeTransactionItem item) {
        return latestSnapshot(item)
                .map(snapshot -> new PricePoint(
                        MoneyCalculationSupport.money(snapshot.getMarketPriceSgd()),
                        snapshot.getConfidenceRating(),
                        true))
                .orElseGet(() -> new PricePoint(BigDecimal.ZERO.setScale(2), ConfidenceRating.LOW, false));
    }

    private java.util.Optional<PriceSnapshot> latestSnapshot(TradeTransactionItem item) {
        if (item.getSealedProduct() != null) {
            return priceSnapshotRepository.findTopBySealedProductIdOrderByCalculatedAtDescIdDesc(
                    item.getSealedProduct().getId());
        }
        return priceSnapshotRepository
                .findTopByCardIdAndCardVariantOrderByCalculatedAtDescIdDesc(
                        item.getCard().getId(),
                        tradeItemVariant(item))
                .or(() -> priceSnapshotRepository
                        .findTopByCardIdAndCardVariantIsNullOrderByCalculatedAtDescIdDesc(item.getCard().getId()));
    }

    private CardVariant tradeItemVariant(TradeTransactionItem item) {
        if (item.getOutgoingOwnedItem() != null) {
            return item.getOutgoingOwnedItem().getOwnedVariant();
        }
        return item.getIncomingVariant();
    }

    private BigDecimal baseValue(TradeTransactionItem item, PricePoint pricePoint) {
        if (item.getOverrideValueSgd() != null) {
            return MoneyCalculationSupport.money(item.getOverrideValueSgd());
        }
        if (!pricePoint.hasMarketValue()) {
            if (item.getSideType() == TradeSideType.INCOMING) {
                throw new IllegalArgumentException(
                        "No market value found for this incoming card. Enter an Override Value SGD to continue.");
            }
            throw new IllegalArgumentException(
                    "No market value found. Add a manual price entry or provide an override value.");
        }
        return pricePoint.marketValueSgd();
    }

    private ConfidenceRating overallConfidence(
            List<TradeTransactionItem> outgoingItems,
            List<TradeTransactionItem> incomingItems) {
        List<ConfidenceRating> confidenceRatings = java.util.stream.Stream.concat(
                        outgoingItems.stream(),
                        incomingItems.stream())
                .map(TradeTransactionItem::getConfidenceRating)
                .toList();
        if (confidenceRatings.isEmpty() || confidenceRatings.contains(ConfidenceRating.LOW)) {
            return ConfidenceRating.LOW;
        }
        if (confidenceRatings.contains(ConfidenceRating.MEDIUM)) {
            return ConfidenceRating.MEDIUM;
        }
        return ConfidenceRating.HIGH;
    }

    private TradeFairnessResult fairness(BigDecimal netDifference) {
        if (netDifference.abs().compareTo(BALANCED_TOLERANCE_SGD) <= 0) {
            return TradeFairnessResult.BALANCED;
        }
        return netDifference.compareTo(BigDecimal.ZERO) > 0
                ? TradeFairnessResult.FAVORABLE
                : TradeFairnessResult.UNFAVORABLE;
    }

    private record SideTotals(
            BigDecimal marketValueSgd,
            BigDecimal agreedValueSgd,
            BigDecimal adjustedValueSgd) {
    }

    private record PricePoint(BigDecimal marketValueSgd, ConfidenceRating confidenceRating, boolean hasMarketValue) {
    }
}
