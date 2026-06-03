package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.GradedStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class OwnedItemForm {

    @NotNull
    private Long cardId;

    @NotNull
    private CardCondition condition = CardCondition.RAW_NEAR_MINT;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal purchasePriceSgd;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate purchaseDate = LocalDate.now();

    @NotNull
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

    public CardCondition getCondition() {
        return condition;
    }

    public void setCondition(CardCondition condition) {
        this.condition = condition;
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

