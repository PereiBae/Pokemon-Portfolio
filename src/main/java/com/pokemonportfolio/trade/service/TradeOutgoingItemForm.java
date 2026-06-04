package com.pokemonportfolio.trade.service;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TradeOutgoingItemForm {

    @NotNull
    private Long ownedItemId;

    private BigDecimal overrideValueSgd;

    private String notes;

    public Long getOwnedItemId() {
        return ownedItemId;
    }

    public void setOwnedItemId(Long ownedItemId) {
        this.ownedItemId = ownedItemId;
    }

    public BigDecimal getOverrideValueSgd() {
        return overrideValueSgd;
    }

    public void setOverrideValueSgd(BigDecimal overrideValueSgd) {
        this.overrideValueSgd = overrideValueSgd;
    }

    public BigDecimal getAgreedValueSgd() {
        return overrideValueSgd;
    }

    public void setAgreedValueSgd(BigDecimal agreedValueSgd) {
        this.overrideValueSgd = agreedValueSgd;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
