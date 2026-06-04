package com.pokemonportfolio.trade.service;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public class TradePercentageForm {

    @DecimalMin("0.01")
    private BigDecimal outgoingTradePercentage = new BigDecimal("100.00");

    @DecimalMin("0.01")
    private BigDecimal incomingTradePercentage = new BigDecimal("100.00");

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
}
