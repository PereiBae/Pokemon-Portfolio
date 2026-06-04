package com.pokemonportfolio.trade.entity;

import com.pokemonportfolio.config.domain.TradeSideType;
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

@Entity
@Table(name = "trade_transaction_side")
public class TradeTransactionSide extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_transaction_id", nullable = false)
    private TradeTransaction tradeTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "side_type", nullable = false)
    private TradeSideType sideType;

    @Column(name = "trade_percentage", nullable = false, precision = 9, scale = 4)
    private BigDecimal tradePercentage = new BigDecimal("100.0000");

    @Column(name = "total_market_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalMarketValueSgd = BigDecimal.ZERO;

    @Column(name = "total_agreed_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAgreedValueSgd = BigDecimal.ZERO;

    @Column(name = "total_adjusted_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAdjustedValueSgd = BigDecimal.ZERO;

    protected TradeTransactionSide() {
    }

    public TradeTransactionSide(TradeTransaction tradeTransaction, TradeSideType sideType, BigDecimal tradePercentage) {
        this.tradeTransaction = tradeTransaction;
        this.sideType = sideType;
        this.tradePercentage = tradePercentage;
    }

    public Long getId() {
        return id;
    }

    public TradeTransaction getTradeTransaction() {
        return tradeTransaction;
    }

    public TradeSideType getSideType() {
        return sideType;
    }

    public BigDecimal getTradePercentage() {
        return tradePercentage;
    }

    public BigDecimal getTotalMarketValueSgd() {
        return totalMarketValueSgd;
    }

    public BigDecimal getTotalAgreedValueSgd() {
        return totalAgreedValueSgd;
    }

    public BigDecimal getTotalAdjustedValueSgd() {
        return totalAdjustedValueSgd;
    }

    public void updateTradePercentage(BigDecimal tradePercentage) {
        this.tradePercentage = tradePercentage;
    }

    public void updateTotals(
            BigDecimal totalMarketValueSgd,
            BigDecimal totalAgreedValueSgd,
            BigDecimal totalAdjustedValueSgd) {
        this.totalMarketValueSgd = totalMarketValueSgd;
        this.totalAgreedValueSgd = totalAgreedValueSgd;
        this.totalAdjustedValueSgd = totalAdjustedValueSgd;
    }
}
