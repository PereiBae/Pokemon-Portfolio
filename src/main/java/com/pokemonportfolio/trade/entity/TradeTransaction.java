package com.pokemonportfolio.trade.entity;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.TradeFairnessResult;
import com.pokemonportfolio.config.domain.TradeTransactionMode;
import com.pokemonportfolio.config.domain.TradeTransactionStatus;
import com.pokemonportfolio.config.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "trade_transaction")
public class TradeTransaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser owner;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeTransactionMode mode = TradeTransactionMode.ANALYSIS_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeTransactionStatus status = TradeTransactionStatus.DRAFT;

    @Column(name = "total_outgoing_market_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalOutgoingMarketValueSgd = BigDecimal.ZERO;

    @Column(name = "total_incoming_market_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalIncomingMarketValueSgd = BigDecimal.ZERO;

    @Column(name = "total_outgoing_agreed_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalOutgoingAgreedValueSgd = BigDecimal.ZERO;

    @Column(name = "total_incoming_agreed_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalIncomingAgreedValueSgd = BigDecimal.ZERO;

    @Column(name = "total_outgoing_adjusted_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalOutgoingAdjustedValueSgd = BigDecimal.ZERO;

    @Column(name = "total_incoming_adjusted_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalIncomingAdjustedValueSgd = BigDecimal.ZERO;

    @Column(name = "net_difference_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal netDifferenceSgd = BigDecimal.ZERO;

    @Column(name = "trade_imbalance_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal tradeImbalanceSgd = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "fairness_result", nullable = false)
    private TradeFairnessResult fairnessResult = TradeFairnessResult.BALANCED;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_rating", nullable = false)
    private ConfidenceRating confidenceRating = ConfidenceRating.LOW;

    @Column(length = 1000)
    private String notes;

    @Column(name = "analysed_at")
    private OffsetDateTime analysedAt;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    protected TradeTransaction() {
    }

    public TradeTransaction(AppUser owner, String name, String notes) {
        this.owner = owner;
        this.name = name;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public TradeTransactionMode getMode() {
        return mode;
    }

    public TradeTransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalOutgoingMarketValueSgd() {
        return totalOutgoingMarketValueSgd;
    }

    public BigDecimal getTotalIncomingMarketValueSgd() {
        return totalIncomingMarketValueSgd;
    }

    public BigDecimal getTotalOutgoingAgreedValueSgd() {
        return totalOutgoingAgreedValueSgd;
    }

    public BigDecimal getTotalIncomingAgreedValueSgd() {
        return totalIncomingAgreedValueSgd;
    }

    public BigDecimal getTotalOutgoingAdjustedValueSgd() {
        return totalOutgoingAdjustedValueSgd;
    }

    public BigDecimal getTotalIncomingAdjustedValueSgd() {
        return totalIncomingAdjustedValueSgd;
    }

    public BigDecimal getNetDifferenceSgd() {
        return netDifferenceSgd;
    }

    public BigDecimal getTradeImbalanceSgd() {
        return tradeImbalanceSgd;
    }

    public TradeFairnessResult getFairnessResult() {
        return fairnessResult;
    }

    public ConfidenceRating getConfidenceRating() {
        return confidenceRating;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getAnalysedAt() {
        return analysedAt;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public boolean isExecuted() {
        return status == TradeTransactionStatus.EXECUTED;
    }

    public boolean isCancelled() {
        return status == TradeTransactionStatus.CANCELLED;
    }

    public void updateAnalysis(
            BigDecimal totalOutgoingMarketValueSgd,
            BigDecimal totalIncomingMarketValueSgd,
            BigDecimal totalOutgoingAgreedValueSgd,
            BigDecimal totalIncomingAgreedValueSgd,
            BigDecimal totalOutgoingAdjustedValueSgd,
            BigDecimal totalIncomingAdjustedValueSgd,
            BigDecimal netDifferenceSgd,
            BigDecimal tradeImbalanceSgd,
            TradeFairnessResult fairnessResult,
            ConfidenceRating confidenceRating,
            OffsetDateTime analysedAt) {
        this.totalOutgoingMarketValueSgd = totalOutgoingMarketValueSgd;
        this.totalIncomingMarketValueSgd = totalIncomingMarketValueSgd;
        this.totalOutgoingAgreedValueSgd = totalOutgoingAgreedValueSgd;
        this.totalIncomingAgreedValueSgd = totalIncomingAgreedValueSgd;
        this.totalOutgoingAdjustedValueSgd = totalOutgoingAdjustedValueSgd;
        this.totalIncomingAdjustedValueSgd = totalIncomingAdjustedValueSgd;
        this.netDifferenceSgd = netDifferenceSgd;
        this.tradeImbalanceSgd = tradeImbalanceSgd;
        this.fairnessResult = fairnessResult;
        this.confidenceRating = confidenceRating;
        this.status = TradeTransactionStatus.ANALYSED;
        this.analysedAt = analysedAt;
    }

    public void markExecuted(OffsetDateTime executedAt) {
        this.mode = TradeTransactionMode.EXECUTE_TRADE;
        this.status = TradeTransactionStatus.EXECUTED;
        this.executedAt = executedAt;
    }
}
