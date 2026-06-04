package com.pokemonportfolio.portfolio.entity;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
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
import java.time.OffsetDateTime;

@Entity
@Table(name = "owned_item")
public class OwnedItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(name = "owned_variant", nullable = false)
    private CardVariant ownedVariant;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", nullable = false)
    private CardCondition condition;

    @Column(name = "purchase_price_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal purchasePriceSgd;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "graded_status", nullable = false)
    private GradedStatus gradedStatus;

    @Column(name = "psa_grade")
    private Integer psaGrade;

    @Column(name = "psa_certification_number")
    private String psaCertificationNumber;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    private OwnedItemStatus status = OwnedItemStatus.ACTIVE;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    protected OwnedItem() {
    }

    public OwnedItem(
            AppUser owner,
            Card card,
            CardVariant ownedVariant,
            CardCondition condition,
            BigDecimal purchasePriceSgd,
            LocalDate purchaseDate,
            GradedStatus gradedStatus,
            Integer psaGrade,
            String psaCertificationNumber,
            String notes) {
        this.owner = owner;
        this.card = card;
        this.ownedVariant = ownedVariant;
        this.condition = condition;
        this.purchasePriceSgd = purchasePriceSgd;
        this.purchaseDate = purchaseDate;
        this.gradedStatus = gradedStatus;
        this.psaGrade = psaGrade;
        this.psaCertificationNumber = psaCertificationNumber;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public Card getCard() {
        return card;
    }

    public CardVariant getOwnedVariant() {
        return ownedVariant;
    }

    public CardCondition getCondition() {
        return condition;
    }

    public BigDecimal getPurchasePriceSgd() {
        return purchasePriceSgd;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public GradedStatus getGradedStatus() {
        return gradedStatus;
    }

    public Integer getPsaGrade() {
        return psaGrade;
    }

    public String getPsaCertificationNumber() {
        return psaCertificationNumber;
    }

    public String getNotes() {
        return notes;
    }

    public OwnedItemStatus getStatus() {
        return status;
    }

    public OffsetDateTime getArchivedAt() {
        return archivedAt;
    }

    public boolean isActive() {
        return status == OwnedItemStatus.ACTIVE;
    }

    public void markSold(OffsetDateTime disposedAt) {
        markDisposed(OwnedItemStatus.SOLD, disposedAt);
    }

    public void markTraded(OffsetDateTime disposedAt) {
        markDisposed(OwnedItemStatus.TRADED, disposedAt);
    }

    public void markDeleted(OffsetDateTime disposedAt) {
        markDisposed(OwnedItemStatus.DELETED, disposedAt);
    }

    private void markDisposed(OwnedItemStatus newStatus, OffsetDateTime disposedAt) {
        this.status = newStatus;
        this.archivedAt = disposedAt;
    }
}
