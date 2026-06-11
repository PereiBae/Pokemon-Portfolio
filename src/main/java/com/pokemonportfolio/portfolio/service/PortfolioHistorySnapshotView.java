package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.portfolio.entity.PortfolioValuationSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PortfolioHistorySnapshotView(
        Long id,
        BigDecimal totalValueSgd,
        BigDecimal totalCostBasisSgd,
        BigDecimal unrealizedGainLossSgd,
        BigDecimal unrealizedGainLossPercent,
        BigDecimal realizedGainLossSgd,
        BigDecimal realizedGainLossPercent,
        BigDecimal totalPerformanceSgd,
        BigDecimal totalPerformancePercent,
        int itemCount,
        int lowConfidenceCount,
        LocalDate snapshotDate,
        OffsetDateTime calculatedAt,
        String explanation) {

    public static PortfolioHistorySnapshotView from(PortfolioValuationSnapshot snapshot) {
        return new PortfolioHistorySnapshotView(
                snapshot.getId(),
                snapshot.getTotalValueSgd(),
                snapshot.getTotalCostBasisSgd(),
                snapshot.getUnrealizedGainLossSgd(),
                snapshot.getUnrealizedGainLossPercent(),
                snapshot.getRealizedGainLossSgd(),
                snapshot.getRealizedGainLossPercent(),
                snapshot.getTotalPerformanceSgd(),
                snapshot.getTotalPerformancePercent(),
                snapshot.getItemCount(),
                snapshot.getLowConfidenceCount(),
                snapshot.getSnapshotDate(),
                snapshot.getCalculatedAt(),
                snapshot.getExplanation());
    }

    public boolean unrealizedGainIsNonNegative() {
        return unrealizedGainLossSgd.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean realizedGainIsNonNegative() {
        return realizedGainLossSgd.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean totalPerformanceIsNonNegative() {
        return totalPerformanceSgd.compareTo(BigDecimal.ZERO) >= 0;
    }
}
