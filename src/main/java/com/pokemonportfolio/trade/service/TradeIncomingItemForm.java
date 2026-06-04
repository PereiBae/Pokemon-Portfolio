package com.pokemonportfolio.trade.service;

import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.SealedProductCondition;
import java.math.BigDecimal;

public class TradeIncomingItemForm {

    private Long cardId;

    private Long sealedProductId;

    private CardVariant variant;

    private CardCondition condition = CardCondition.RAW_NEAR_MINT;

    private SealedProductCondition sealedCondition = SealedProductCondition.SEALED;

    private BigDecimal overrideValueSgd;

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
