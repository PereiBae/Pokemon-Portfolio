package com.pokemonportfolio.trade.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class TradeCreateForm {

    @NotBlank
    private String name;

    @DecimalMin("0.01")
    private BigDecimal outgoingTradePercentage = new BigDecimal("100.00");

    @DecimalMin("0.01")
    private BigDecimal incomingTradePercentage = new BigDecimal("100.00");

    private String notes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getOutgoingTradePercentage() {
        return outgoingTradePercentage;
    }

    public void setOutgoingTradePercentage(BigDecimal outgoingTradePercentage) {
        this.outgoingTradePercentage = outgoingTradePercentage;
    }

    public BigDecimal getIncomingTradePercentage() {
        return incomingTradePercentage;
    }

    public void setIncomingTradePercentage(BigDecimal incomingTradePercentage) {
        this.incomingTradePercentage = incomingTradePercentage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
