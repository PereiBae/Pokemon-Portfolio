package com.pokemonportfolio.config.domain;

public enum SealedProductType {
    BOOSTER_BOX("Booster Box"),
    BOOSTER_PACK("Booster Pack"),
    ETB("Elite Trainer Box"),
    COLLECTION_BOX("Collection Box"),
    PROMO_BOX("Promo Box"),
    OTHER("Other");

    private final String label;

    SealedProductType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
