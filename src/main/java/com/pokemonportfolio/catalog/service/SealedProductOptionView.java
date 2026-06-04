package com.pokemonportfolio.catalog.service;

import com.pokemonportfolio.catalog.entity.SealedProduct;

public record SealedProductOptionView(
        Long id,
        String name,
        String productTypeLabel,
        String languageMarketLabel,
        String setName,
        String imageUrl,
        String verificationStatusLabel,
        String displayLabel) {

    public static SealedProductOptionView from(SealedProduct product) {
        String set = product.getSetName() == null || product.getSetName().isBlank()
                ? "No set"
                : product.getSetName();
        return new SealedProductOptionView(
                product.getId(),
                product.getName(),
                product.getProductType().getLabel(),
                product.getLanguageMarket().getLabel(),
                set,
                product.getImageUrl(),
                product.getVerificationStatus().getLabel(),
                product.getName() + " - " + set + " [" + product.getProductType().getLabel() + "]");
    }
}
