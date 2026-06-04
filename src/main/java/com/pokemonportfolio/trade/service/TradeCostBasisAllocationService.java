package com.pokemonportfolio.trade.service;

import com.pokemonportfolio.trade.entity.TradeTransactionItem;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class TradeCostBasisAllocationService {

    public Map<TradeTransactionItem, BigDecimal> allocateIncomingCostBasis(List<TradeTransactionItem> incomingItems) {
        LinkedHashMap<TradeTransactionItem, BigDecimal> allocations = new LinkedHashMap<>();
        for (TradeTransactionItem incomingItem : incomingItems) {
            allocations.put(incomingItem, MoneyCalculationSupport.money(incomingItem.getAdjustedValueSgd()));
        }
        return allocations;
    }

    public Map<TradeTransactionItem, BigDecimal> allocateTradeValueReceived(
            List<TradeTransactionItem> outgoingItems,
            BigDecimal totalIncomingAdjustedValueSgd) {
        return allocateByWeight(
                outgoingItems,
                totalIncomingAdjustedValueSgd,
                TradeTransactionItem::getAdjustedValueSgd);
    }

    private Map<TradeTransactionItem, BigDecimal> allocateByWeight(
            List<TradeTransactionItem> items,
            BigDecimal totalToAllocate,
            Function<TradeTransactionItem, BigDecimal> weightExtractor) {
        if (items.isEmpty()) {
            return Map.of();
        }
        BigDecimal normalizedTotal = MoneyCalculationSupport.money(totalToAllocate);
        BigDecimal totalWeight = items.stream()
                .map(weightExtractor)
                .map(MoneyCalculationSupport::money)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LinkedHashMap<TradeTransactionItem, BigDecimal> allocations = new LinkedHashMap<>();
        BigDecimal remaining = normalizedTotal;
        for (int index = 0; index < items.size(); index++) {
            TradeTransactionItem item = items.get(index);
            BigDecimal allocation;
            if (index == items.size() - 1) {
                allocation = remaining;
            } else if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
                allocation = normalizedTotal
                        .divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
            } else {
                allocation = normalizedTotal
                        .multiply(MoneyCalculationSupport.money(weightExtractor.apply(item)))
                        .divide(totalWeight, 2, RoundingMode.HALF_UP);
            }
            allocation = MoneyCalculationSupport.money(allocation);
            allocations.put(item, allocation);
            remaining = MoneyCalculationSupport.money(remaining.subtract(allocation));
        }
        return allocations;
    }
}
