package com.pokemonportfolio.catalog.service;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.VerificationStatus;
import java.util.List;
import java.util.stream.Collectors;

public class CardOptionView {

    private final Long id;
    private final String name;
    private final String setName;
    private final String cardNumber;
    private final CardVariant variant;
    private final VerificationStatus verificationStatus;
    private final CatalogSource catalogSource;
    private final String imageSmallUrl;
    private final String imageLargeUrl;
    private final List<CardVariant> availableVariants;
    private final String displayLabel;

    private CardOptionView(
            Long id,
            String name,
            String setName,
            String cardNumber,
            CardVariant variant,
            VerificationStatus verificationStatus,
            CatalogSource catalogSource,
            String imageSmallUrl,
            String imageLargeUrl,
            List<CardVariant> availableVariants) {
        this.id = id;
        this.name = name;
        this.setName = setName;
        this.cardNumber = cardNumber;
        this.variant = variant;
        this.verificationStatus = verificationStatus;
        this.catalogSource = catalogSource;
        this.imageSmallUrl = imageSmallUrl;
        this.imageLargeUrl = imageLargeUrl;
        this.availableVariants = List.copyOf(availableVariants);
        this.displayLabel = buildDisplayLabel(name, setName, cardNumber, variant, verificationStatus);
    }

    public static CardOptionView from(Card card) {
        return new CardOptionView(
                card.getId(),
                card.getName(),
                card.getPokemonSet().getName(),
                card.getCardNumber(),
                card.getVariant(),
                card.getVerificationStatus(),
                card.getCatalogSource(),
                card.getExternalImageSmallUrl(),
                card.getExternalImageLargeUrl(),
                card.getAvailableVariants());
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

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public CatalogSource getCatalogSource() {
        return catalogSource;
    }

    public String getImageSmallUrl() {
        return imageSmallUrl;
    }

    public String getImageLargeUrl() {
        return imageLargeUrl;
    }

    public List<CardVariant> getAvailableVariants() {
        return availableVariants;
    }

    public String getAvailableVariantLabels() {
        return availableVariants.stream()
                .map(CardVariant::getLabel)
                .collect(Collectors.joining(", "));
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    private static String buildDisplayLabel(
            String name,
            String setName,
            String cardNumber,
            CardVariant variant,
            VerificationStatus verificationStatus) {
        String label = name + " #" + cardNumber + " - " + setName;
        if (variant != null && variant != CardVariant.STANDARD) {
            label = label + " (" + variant.getLabel() + ")";
        }
        return label + " [" + verificationStatus.getLabel() + "]";
    }
}
