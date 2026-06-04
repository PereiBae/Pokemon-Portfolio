package com.pokemonportfolio.trade.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.TradeFairnessResult;
import com.pokemonportfolio.config.domain.TradeTransactionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TradeTransactionListView(
        Long id,
        String name,
        TradeTransactionStatus status,
        String statusLabel,
        TradeFairnessResult fairnessResult,
        String fairnessLabel,
        ConfidenceRating confidenceRating,
        BigDecimal outgoingAdjustedValueSgd,
        BigDecimal incomingAdjustedValueSgd,
        BigDecimal netDifferenceSgd,
        BigDecimal tradeImbalanceSgd,
        OffsetDateTime createdAt,
        OffsetDateTime executedAt) {

    public boolean netDifferenceIsNonNegative() {
        return netDifferenceSgd.compareTo(BigDecimal.ZERO) >= 0;
    }
}
