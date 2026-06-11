package com.pokemonportfolio.portfolio.entity;

import com.pokemonportfolio.auth.entity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "portfolio_valuation_snapshot")
public class PortfolioValuationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser owner;

    @Column(name = "total_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalValueSgd;

    @Column(name = "total_cost_basis_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCostBasisSgd;

    @Column(name = "unrealized_gain_loss_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal unrealizedGainLossSgd;

    @Column(name = "unrealized_gain_loss_percent", nullable = false, precision = 9, scale = 4)
    private BigDecimal unrealizedGainLossPercent;

    @Column(name = "realized_gain_loss_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal realizedGainLossSgd;

    @Column(name = "realized_gain_loss_percent", nullable = false, precision = 9, scale = 4)
    private BigDecimal realizedGainLossPercent;

    @Column(name = "realized_cost_basis_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal realizedCostBasisSgd;

    @Column(name = "total_performance_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPerformanceSgd;

    @Column(name = "total_performance_percent", nullable = false, precision = 9, scale = 4)
    private BigDecimal totalPerformancePercent;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "low_confidence_count", nullable = false)
    private int lowConfidenceCount;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @Column(nullable = false, length = 1000)
    private String explanation;

    protected PortfolioValuationSnapshot() {
    }

    public PortfolioValuationSnapshot(
            AppUser owner,
            BigDecimal totalValueSgd,
            BigDecimal totalCostBasisSgd,
            BigDecimal unrealizedGainLossSgd,
            BigDecimal unrealizedGainLossPercent,
            BigDecimal realizedGainLossSgd,
            BigDecimal realizedGainLossPercent,
            BigDecimal realizedCostBasisSgd,
            BigDecimal totalPerformanceSgd,
            BigDecimal totalPerformancePercent,
            int itemCount,
            int lowConfidenceCount,
            LocalDate snapshotDate,
            OffsetDateTime calculatedAt,
            String explanation) {
        this.owner = owner;
        this.totalValueSgd = totalValueSgd;
        this.totalCostBasisSgd = totalCostBasisSgd;
        this.unrealizedGainLossSgd = unrealizedGainLossSgd;
        this.unrealizedGainLossPercent = unrealizedGainLossPercent;
        this.realizedGainLossSgd = realizedGainLossSgd;
        this.realizedGainLossPercent = realizedGainLossPercent;
        this.realizedCostBasisSgd = realizedCostBasisSgd;
        this.totalPerformanceSgd = totalPerformanceSgd;
        this.totalPerformancePercent = totalPerformancePercent;
        this.itemCount = itemCount;
        this.lowConfidenceCount = lowConfidenceCount;
        this.snapshotDate = snapshotDate;
        this.calculatedAt = calculatedAt;
        this.explanation = explanation;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getTotalValueSgd() {
        return totalValueSgd;
    }

    public BigDecimal getTotalCostBasisSgd() {
        return totalCostBasisSgd;
    }

    public BigDecimal getUnrealizedGainLossSgd() {
        return unrealizedGainLossSgd;
    }

    public BigDecimal getUnrealizedGainLossPercent() {
        return unrealizedGainLossPercent;
    }

    public BigDecimal getRealizedGainLossSgd() {
        return realizedGainLossSgd;
    }

    public BigDecimal getRealizedGainLossPercent() {
        return realizedGainLossPercent;
    }

    public BigDecimal getRealizedCostBasisSgd() {
        return realizedCostBasisSgd;
    }

    public BigDecimal getTotalPerformanceSgd() {
        return totalPerformanceSgd;
    }

    public BigDecimal getTotalPerformancePercent() {
        return totalPerformancePercent;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getLowConfidenceCount() {
        return lowConfidenceCount;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public OffsetDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public String getExplanation() {
        return explanation;
    }
}
