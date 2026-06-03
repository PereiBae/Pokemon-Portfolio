package com.pokemonportfolio.catalog.service;

import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.LanguageMarket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CardForm {

    @NotBlank
    private String name;

    @NotBlank
    private String setName;

    @NotBlank
    private String cardNumber;

    @NotNull
    private LanguageMarket languageMarket = LanguageMarket.ENGLISH;

    @NotNull
    private CardVariant variant = CardVariant.STANDARD;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public LanguageMarket getLanguageMarket() {
        return languageMarket;
    }

    public void setLanguageMarket(LanguageMarket languageMarket) {
        this.languageMarket = languageMarket;
    }

    public CardVariant getVariant() {
        return variant;
    }

    public void setVariant(CardVariant variant) {
        this.variant = variant;
    }
}

