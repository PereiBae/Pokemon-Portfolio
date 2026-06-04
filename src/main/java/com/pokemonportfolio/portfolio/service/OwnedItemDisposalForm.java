package com.pokemonportfolio.portfolio.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class OwnedItemDisposalForm {

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate disposalDate = LocalDate.now();

    @DecimalMin("0.00")
    private BigDecimal salePriceSgd;

    @DecimalMin("0.00")
    private BigDecimal tradeValueSgd;

    @DecimalMin("0.00")
    private BigDecimal feesSgd = BigDecimal.ZERO;

    private String notes;

    public LocalDate getDisposalDate() {
        return disposalDate;
    }

    public void setDisposalDate(LocalDate disposalDate) {
        this.disposalDate = disposalDate;
    }

    public BigDecimal getSalePriceSgd() {
        return salePriceSgd;
    }

    public void setSalePriceSgd(BigDecimal salePriceSgd) {
        this.salePriceSgd = salePriceSgd;
    }

    public BigDecimal getTradeValueSgd() {
        return tradeValueSgd;
    }

    public void setTradeValueSgd(BigDecimal tradeValueSgd) {
        this.tradeValueSgd = tradeValueSgd;
    }

    public BigDecimal getFeesSgd() {
        return feesSgd;
    }

    public void setFeesSgd(BigDecimal feesSgd) {
        this.feesSgd = feesSgd;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
