package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.portfolio.entity.OwnedItem;

public record OwnedItemOptionView(
        Long ownedItemId,
        Long cardId,
        String displayLabel) {

    public static OwnedItemOptionView from(OwnedItem ownedItem) {
        String label = ownedItem.getCard().getName()
                + " #"
                + ownedItem.getCard().getCardNumber()
                + " - "
                + ownedItem.getCard().getPokemonSet().getName()
                + " ["
                + ownedItem.getCard().getVerificationStatus().getLabel()
                + "]"
                + " / "
                + ownedItem.getCondition().getLabel();
        return new OwnedItemOptionView(ownedItem.getId(), ownedItem.getCard().getId(), label);
    }
}
