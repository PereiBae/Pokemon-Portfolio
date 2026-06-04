package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.portfolio.entity.OwnedItem;

public record OwnedItemOptionView(
        Long ownedItemId,
        Long cardId,
        Long sealedProductId,
        String assetTypeLabel,
        String displayLabel) {

    public static OwnedItemOptionView from(OwnedItem ownedItem) {
        String label = ownedItem.displayName()
                + " ["
                + ownedItem.verificationStatusLabel()
                + "] / "
                + ownedItem.conditionLabel();
        Long cardId = ownedItem.getCard() == null ? null : ownedItem.getCard().getId();
        Long sealedProductId = ownedItem.getSealedProduct() == null ? null : ownedItem.getSealedProduct().getId();
        return new OwnedItemOptionView(
                ownedItem.getId(),
                cardId,
                sealedProductId,
                ownedItem.getAssetType().getLabel(),
                label);
    }
}
