package com.pokemonportfolio.portfolio.entity;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.DisposalType;
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
import java.time.LocalDate;

@Entity
@Table(name = "owned_item_disposal")
public class OwnedItemDisposal extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owned_item_id", nullable = false)
    private OwnedItem ownedItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposal_type", nullable = false)
    private DisposalType disposalType;

    @Column(name = "disposal_date", nullable = false)
    private LocalDate disposalDate;

    @Column(name = "proceeds_value_sgd", precision = 19, scale = 2)
    private BigDecimal proceedsValueSgd;

    @Column(name = "fees_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal feesSgd;

    @Column(name = "net_proceeds_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal netProceedsSgd;

    @Column(name = "original_purchase_price_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalPurchasePriceSgd;

    @Column(name = "realized_gain_loss_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal realizedGainLossSgd;

    @Column(name = "realized_gain_loss_percent", nullable = false, precision = 19, scale = 4)
    private BigDecimal realizedGainLossPercent;

    @Column(length = 1000)
    private String notes;

    @Column(name = "trade_transaction_id")
    private Long tradeTransactionId;

    protected OwnedItemDisposal() {
    }

    public OwnedItemDisposal(
            AppUser owner,
            OwnedItem ownedItem,
            DisposalType disposalType,
            LocalDate disposalDate,
            BigDecimal proceedsValueSgd,
            BigDecimal feesSgd,
            BigDecimal netProceedsSgd,
            BigDecimal originalPurchasePriceSgd,
            BigDecimal realizedGainLossSgd,
            BigDecimal realizedGainLossPercent,
            String notes) {
        this(
                owner,
                ownedItem,
                disposalType,
                disposalDate,
                proceedsValueSgd,
                feesSgd,
                netProceedsSgd,
                originalPurchasePriceSgd,
                realizedGainLossSgd,
                realizedGainLossPercent,
                notes,
                null);
    }

    public OwnedItemDisposal(
            AppUser owner,
            OwnedItem ownedItem,
            DisposalType disposalType,
            LocalDate disposalDate,
            BigDecimal proceedsValueSgd,
            BigDecimal feesSgd,
            BigDecimal netProceedsSgd,
            BigDecimal originalPurchasePriceSgd,
            BigDecimal realizedGainLossSgd,
            BigDecimal realizedGainLossPercent,
            String notes,
            Long tradeTransactionId) {
        this.owner = owner;
        this.ownedItem = ownedItem;
        this.disposalType = disposalType;
        this.disposalDate = disposalDate;
        this.proceedsValueSgd = proceedsValueSgd;
        this.feesSgd = feesSgd;
        this.netProceedsSgd = netProceedsSgd;
        this.originalPurchasePriceSgd = originalPurchasePriceSgd;
        this.realizedGainLossSgd = realizedGainLossSgd;
        this.realizedGainLossPercent = realizedGainLossPercent;
        this.notes = notes;
        this.tradeTransactionId = tradeTransactionId;
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public OwnedItem getOwnedItem() {
        return ownedItem;
    }

    public DisposalType getDisposalType() {
        return disposalType;
    }

    public LocalDate getDisposalDate() {
        return disposalDate;
    }

    public BigDecimal getProceedsValueSgd() {
        return proceedsValueSgd;
    }

    public BigDecimal getFeesSgd() {
        return feesSgd;
    }

    public BigDecimal getNetProceedsSgd() {
        return netProceedsSgd;
    }

    public BigDecimal getOriginalPurchasePriceSgd() {
        return originalPurchasePriceSgd;
    }

    public BigDecimal getRealizedGainLossSgd() {
        return realizedGainLossSgd;
    }

    public BigDecimal getRealizedGainLossPercent() {
        return realizedGainLossPercent;
    }

    public String getNotes() {
        return notes;
    }

    public Long getTradeTransactionId() {
        return tradeTransactionId;
    }
}
