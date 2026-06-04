package com.pokemonportfolio.portfolio.entity;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.config.domain.AssetType;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.config.domain.SealedProductCondition;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType = AssetType.CARD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sealed_product_id")
    private SealedProduct sealedProduct;

    @Enumerated(EnumType.STRING)
    @Column(name = "owned_variant", nullable = false)
    private CardVariant ownedVariant;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition")
    private CardCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "sealed_condition")
    private SealedProductCondition sealedCondition;

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
        this.assetType = AssetType.CARD;
        this.card = card;
        this.ownedVariant = ownedVariant;
        this.condition = condition;
        this.sealedCondition = null;
        this.purchasePriceSgd = purchasePriceSgd;
        this.purchaseDate = purchaseDate;
        this.gradedStatus = gradedStatus;
        this.psaGrade = psaGrade;
        this.psaCertificationNumber = psaCertificationNumber;
        this.notes = notes;
    }

    public OwnedItem(
            AppUser owner,
            SealedProduct sealedProduct,
            SealedProductCondition sealedCondition,
            BigDecimal purchasePriceSgd,
            LocalDate purchaseDate,
            String notes) {
        this.owner = owner;
        this.assetType = AssetType.SEALED_PRODUCT;
        this.sealedProduct = sealedProduct;
        this.ownedVariant = CardVariant.STANDARD;
        this.condition = null;
        this.sealedCondition = sealedCondition;
        this.purchasePriceSgd = purchasePriceSgd;
        this.purchaseDate = purchaseDate;
        this.gradedStatus = GradedStatus.UNGRADED;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public Card getCard() {
        return card;
    }

    public SealedProduct getSealedProduct() {
        return sealedProduct;
    }

    public CardVariant getOwnedVariant() {
        return ownedVariant;
    }

    public CardCondition getCondition() {
        return condition;
    }

    public SealedProductCondition getSealedCondition() {
        return sealedCondition;
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

    public boolean isCard() {
        return assetType == AssetType.CARD;
    }

    public boolean isSealedProduct() {
        return assetType == AssetType.SEALED_PRODUCT;
    }

    public String displayName() {
        if (isSealedProduct()) {
            return sealedProduct.getName() + sealedSetSuffix();
        }
        return card.getName()
                + " #"
                + card.getCardNumber()
                + " - "
                + card.getPokemonSet().getName()
                + " ("
                + ownedVariant.getLabel()
                + ")";
    }

    public String assetName() {
        return isSealedProduct() ? sealedProduct.getName() : card.getName();
    }

    public String assetNumber() {
        return isSealedProduct() ? "" : card.getCardNumber();
    }

    public String setName() {
        if (isSealedProduct()) {
            return sealedProduct.getSetName() == null || sealedProduct.getSetName().isBlank()
                    ? "No set"
                    : sealedProduct.getSetName();
        }
        return card.getPokemonSet().getName();
    }

    public String variantOrTypeLabel() {
        return isSealedProduct() ? sealedProduct.getProductType().getLabel() : ownedVariant.getLabel();
    }

    public String verificationStatusLabel() {
        return isSealedProduct()
                ? sealedProduct.getVerificationStatus().getLabel()
                : card.getVerificationStatus().getLabel();
    }

    public String imageSmallUrl() {
        return isSealedProduct() ? sealedProduct.getImageUrl() : card.getExternalImageSmallUrl();
    }

    public String conditionLabel() {
        return isSealedProduct() ? sealedCondition.getLabel() : condition.getLabel();
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

    private String sealedSetSuffix() {
        if (sealedProduct.getSetName() == null || sealedProduct.getSetName().isBlank()) {
            return "";
        }
        return " - " + sealedProduct.getSetName();
    }
}
