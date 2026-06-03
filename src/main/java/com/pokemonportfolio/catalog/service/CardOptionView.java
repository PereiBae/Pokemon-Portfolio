package com.pokemonportfolio.catalog.service;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.CardVariant;

public class CardOptionView {

    private final Long id;
    private final String name;
    private final String setName;
    private final String cardNumber;
    private final CardVariant variant;
    private final String displayLabel;

    private CardOptionView(Long id, String name, String setName, String cardNumber, CardVariant variant) {
        this.id = id;
        this.name = name;
        this.setName = setName;
        this.cardNumber = cardNumber;
        this.variant = variant;
        this.displayLabel = buildDisplayLabel(name, setName, cardNumber, variant);
    }

    public static CardOptionView from(Card card) {
        return new CardOptionView(
                card.getId(),
                card.getName(),
                card.getPokemonSet().getName(),
                card.getCardNumber(),
                card.getVariant());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSetName() {
        return setName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public CardVariant getVariant() {
        return variant;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    private static String buildDisplayLabel(String name, String setName, String cardNumber, CardVariant variant) {
        String label = name + " #" + cardNumber + " - " + setName;
        if (variant == null || variant == CardVariant.STANDARD) {
            return label;
        }
        return label + " (" + variant.getLabel() + ")";
    }
}
