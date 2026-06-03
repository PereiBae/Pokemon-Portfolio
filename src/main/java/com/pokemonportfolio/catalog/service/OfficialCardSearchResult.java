package com.pokemonportfolio.catalog.service;

import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.LanguageMarket;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class OfficialCardSearchResult {

    private final String externalCardId;
    private final String name;
    private final String externalSetId;
    private final String setName;
    private final String setSeries;
    private final LocalDate setReleaseDate;
    private final String cardNumber;
    private final String rarity;
    private final String imageSmallUrl;
    private final String imageLargeUrl;
    private final String externalCardUrl;
    private final LanguageMarket languageMarket;
    private final CatalogSource source;
    private final List<CardVariant> availableVariants;

    public OfficialCardSearchResult(
            String externalCardId,
            String name,
            String externalSetId,
            String setName,
            String setSeries,
            LocalDate setReleaseDate,
            String cardNumber,
            String rarity,
            String imageSmallUrl,
            String imageLargeUrl,
            String externalCardUrl,
            LanguageMarket languageMarket,
            CatalogSource source,
            List<CardVariant> availableVariants) {
        this.externalCardId = externalCardId;
        this.name = name;
        this.externalSetId = externalSetId;
        this.setName = setName;
        this.setSeries = setSeries;
        this.setReleaseDate = setReleaseDate;
        this.cardNumber = cardNumber;
        this.rarity = rarity;
        this.imageSmallUrl = imageSmallUrl;
        this.imageLargeUrl = imageLargeUrl;
        this.externalCardUrl = externalCardUrl;
        this.languageMarket = languageMarket;
        this.source = source;
        this.availableVariants = availableVariants == null ? List.of() : List.copyOf(availableVariants);
    }

    public String getExternalCardId() {
        return externalCardId;
    }

    public String getName() {
        return name;
    }

    public String getExternalSetId() {
        return externalSetId;
    }

    public String getSetName() {
        return setName;
    }

    public String getSetSeries() {
        return setSeries;
    }

    public LocalDate getSetReleaseDate() {
        return setReleaseDate;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getRarity() {
        return rarity;
    }

    public String getImageUrl() {
        return imageSmallUrl;
    }

    public String getImageSmallUrl() {
        return imageSmallUrl;
    }

    public String getImageLargeUrl() {
        return imageLargeUrl;
    }

    public String getExternalCardUrl() {
        return externalCardUrl;
    }

    public LanguageMarket getLanguageMarket() {
        return languageMarket;
    }

    public CatalogSource getSource() {
        return source;
    }

    public String getSourceLabel() {
        return source.getLabel();
    }

    public List<CardVariant> getAvailableVariants() {
        return availableVariants;
    }

    public boolean hasAvailableVariants() {
        return !availableVariants.isEmpty();
    }

    public boolean hasMultipleVariants() {
        return availableVariants.size() > 1;
    }

    public String getAvailableVariantLabels() {
        if (availableVariants.isEmpty()) {
            return "Not provided";
        }
        return availableVariants.stream()
                .map(CardVariant::getLabel)
                .collect(Collectors.joining(", "));
    }
}
