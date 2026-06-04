package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.config.domain.DisposalType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioDisposalView(
        Long disposalId,
        Long ownedItemId,
        String itemDisplayName,
        DisposalType disposalType,
        String disposalTypeLabel,
        LocalDate disposalDate,
        BigDecimal purchasePriceSgd,
        BigDecimal proceedsValueSgd,
        BigDecimal feesSgd,
        BigDecimal netProceedsSgd,
        BigDecimal realizedGainLossSgd,
        BigDecimal realizedGainLossPercent,
        String notes,
        Long tradeTransactionId) {

    public boolean realizedGainIsNonNegative() {
        return realizedGainLossSgd.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean fromTradeTransaction() {
        return tradeTransactionId != null;
    }
}
