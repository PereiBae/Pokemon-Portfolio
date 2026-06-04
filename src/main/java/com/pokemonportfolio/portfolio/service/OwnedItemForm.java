package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.SealedProductCondition;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class OwnedItemForm {

    private Long cardId;
    private Long sealedProductId;

    private CardVariant variant;

    private CardCondition condition = CardCondition.RAW_NEAR_MINT;

    private SealedProductCondition sealedCondition = SealedProductCondition.SEALED;

    @DecimalMin("0.00")
    private BigDecimal purchasePriceSgd;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate purchaseDate = LocalDate.now();

    private GradedStatus gradedStatus = GradedStatus.UNGRADED;

    private Integer psaGrade;
    private String psaCertificationNumber;
    private String notes;

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Long getSealedProductId() {
        return sealedProductId;
    }

    public void setSealedProductId(Long sealedProductId) {
        this.sealedProductId = sealedProductId;
    }

    public CardVariant getVariant() {
        return variant;
    }

    public void setVariant(CardVariant variant) {
        this.variant = variant;
    }

    public CardCondition getCondition() {
        return condition;
    }

    public void setCondition(CardCondition condition) {
        this.condition = condition;
    }

    public SealedProductCondition getSealedCondition() {
        return sealedCondition;
    }

    public void setSealedCondition(SealedProductCondition sealedCondition) {
        this.sealedCondition = sealedCondition;
    }

    public BigDecimal getPurchasePriceSgd() {
        return purchasePriceSgd;
    }

    public void setPurchasePriceSgd(BigDecimal purchasePriceSgd) {
        this.purchasePriceSgd = purchasePriceSgd;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public GradedStatus getGradedStatus() {
        return gradedStatus;
    }

    public void setGradedStatus(GradedStatus gradedStatus) {
        this.gradedStatus = gradedStatus;
    }

    public Integer getPsaGrade() {
        return psaGrade;
    }

    public void setPsaGrade(Integer psaGrade) {
        this.psaGrade = psaGrade;
    }

    public String getPsaCertificationNumber() {
        return psaCertificationNumber;
    }

    public void setPsaCertificationNumber(String psaCertificationNumber) {
        this.psaCertificationNumber = psaCertificationNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
