package com.pokemonportfolio.alerts.entity;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.AlertStatus;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.entity.AuditableEntity;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
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
@Table(name = "alert")
public class Alert extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owned_item_id", nullable = false)
    private OwnedItem ownedItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "price_snapshot_id", nullable = false)
    private PriceSnapshot priceSnapshot;

    @Column(name = "item_display_name", nullable = false, length = 500)
    private String itemDisplayName;

    @Column(name = "purchase_price_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal purchasePriceSgd;

    @Column(name = "current_market_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentMarketValueSgd;

    @Column(name = "gain_amount_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal gainAmountSgd;

    @Column(name = "gain_percentage", nullable = false, precision = 19, scale = 4)
    private BigDecimal gainPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_rating", nullable = false)
    private ConfidenceRating confidenceRating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.ACTIVE;

    @Column(name = "triggered_at", nullable = false)
    private OffsetDateTime triggeredAt;

    @Column(name = "dismissed_at")
    private OffsetDateTime dismissedAt;

    protected Alert() {
    }

    public Alert(
            AppUser owner,
            OwnedItem ownedItem,
            PriceSnapshot priceSnapshot,
            String itemDisplayName,
            BigDecimal purchasePriceSgd,
            BigDecimal currentMarketValueSgd,
            BigDecimal gainAmountSgd,
            BigDecimal gainPercentage,
            ConfidenceRating confidenceRating,
            OffsetDateTime triggeredAt) {
        this.owner = owner;
        this.ownedItem = ownedItem;
        this.priceSnapshot = priceSnapshot;
        this.itemDisplayName = itemDisplayName;
        this.purchasePriceSgd = purchasePriceSgd;
        this.currentMarketValueSgd = currentMarketValueSgd;
        this.gainAmountSgd = gainAmountSgd;
        this.gainPercentage = gainPercentage;
        this.confidenceRating = confidenceRating;
        this.triggeredAt = triggeredAt;
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

    public PriceSnapshot getPriceSnapshot() {
        return priceSnapshot;
    }

    public String getItemDisplayName() {
        return itemDisplayName;
    }

    public BigDecimal getPurchasePriceSgd() {
        return purchasePriceSgd;
    }

    public BigDecimal getCurrentMarketValueSgd() {
        return currentMarketValueSgd;
    }

    public BigDecimal getGainAmountSgd() {
        return gainAmountSgd;
    }

    public BigDecimal getGainPercentage() {
        return gainPercentage;
    }

    public ConfidenceRating getConfidenceRating() {
        return confidenceRating;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public OffsetDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public OffsetDateTime getDismissedAt() {
        return dismissedAt;
    }

    public void dismiss(OffsetDateTime dismissedAt) {
        this.status = AlertStatus.DISMISSED;
        this.dismissedAt = dismissedAt;
    }
}
