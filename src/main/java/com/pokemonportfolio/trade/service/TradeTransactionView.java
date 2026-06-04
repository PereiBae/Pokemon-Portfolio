package com.pokemonportfolio.trade.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.TradeFairnessResult;
import com.pokemonportfolio.config.domain.TradeTransactionMode;
import com.pokemonportfolio.config.domain.TradeTransactionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TradeTransactionView(
        Long id,
        String name,
        TradeTransactionMode mode,
        String modeLabel,
        TradeTransactionStatus status,
        String statusLabel,
        BigDecimal outgoingTradePercentage,
        BigDecimal incomingTradePercentage,
        BigDecimal totalOutgoingMarketValueSgd,
        BigDecimal totalIncomingMarketValueSgd,
        BigDecimal totalOutgoingAgreedValueSgd,
        BigDecimal totalIncomingAgreedValueSgd,
        BigDecimal totalOutgoingAdjustedValueSgd,
        BigDecimal totalIncomingAdjustedValueSgd,
        BigDecimal netDifferenceSgd,
        BigDecimal tradeImbalanceSgd,
        TradeFairnessResult fairnessResult,
        String fairnessLabel,
        ConfidenceRating confidenceRating,
        String notes,
        OffsetDateTime analysedAt,
        OffsetDateTime executedAt,
        List<TradeItemView> outgoingItems,
        List<TradeItemView> incomingItems) {

    public boolean isExecuted() {
        return status == TradeTransactionStatus.EXECUTED;
    }

    public boolean canExecute() {
        return status != TradeTransactionStatus.EXECUTED
                && status != TradeTransactionStatus.CANCELLED
                && !outgoingItems.isEmpty()
                && !incomingItems.isEmpty();
    }

    public boolean netDifferenceIsNonNegative() {
        return netDifferenceSgd.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean imbalanceIsNonNegative() {
        return tradeImbalanceSgd.compareTo(BigDecimal.ZERO) >= 0;
    }
}
