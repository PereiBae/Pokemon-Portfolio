package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ManualPriceEntryForm {

    private Long cardId;
    private Long ownedItemId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal sourcePrice;

    @NotBlank
    @Size(max = 120)
    private String providerName = "MANUAL";

    @NotBlank
    @Pattern(regexp = "[A-Za-z]{3}")
    private String sourceCurrency = CurrencyConversionService.SGD;

    @NotNull
    @DecimalMin("0.00000001")
    private BigDecimal exchangeRateUsed = BigDecimal.ONE;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal marketPriceSgd;

    @NotNull
    private ConfidenceRating confidenceRating = ConfidenceRating.LOW;

    @Size(max = 900)
    private String notes;

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Long getOwnedItemId() {
        return ownedItemId;
    }

    public void setOwnedItemId(Long ownedItemId) {
        this.ownedItemId = ownedItemId;
    }

    public BigDecimal getSourcePrice() {
        return sourcePrice;
    }

    public void setSourcePrice(BigDecimal sourcePrice) {
        this.sourcePrice = sourcePrice;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public void setSourceCurrency(String sourceCurrency) {
        this.sourceCurrency = sourceCurrency;
    }

    public BigDecimal getExchangeRateUsed() {
        return exchangeRateUsed;
    }

    public void setExchangeRateUsed(BigDecimal exchangeRateUsed) {
        this.exchangeRateUsed = exchangeRateUsed;
    }

    public BigDecimal getMarketPriceSgd() {
        return marketPriceSgd;
    }

    public void setMarketPriceSgd(BigDecimal marketPriceSgd) {
        this.marketPriceSgd = marketPriceSgd;
    }

    public ConfidenceRating getConfidenceRating() {
        return confidenceRating;
    }

    public void setConfidenceRating(ConfidenceRating confidenceRating) {
        this.confidenceRating = confidenceRating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
