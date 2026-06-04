package com.pokemonportfolio.catalog.service;

import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.SealedProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class SealedProductForm {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private SealedProductType productType = SealedProductType.BOOSTER_BOX;

    @NotNull
    private LanguageMarket languageMarket = LanguageMarket.ENGLISH;

    @Size(max = 255)
    private String setName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate releaseDate;

    @Size(max = 1000)
    private String imageUrl;

    @Size(max = 1000)
    private String notes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SealedProductType getProductType() {
        return productType;
    }

    public void setProductType(SealedProductType productType) {
        this.productType = productType;
    }

    public LanguageMarket getLanguageMarket() {
        return languageMarket;
    }

    public void setLanguageMarket(LanguageMarket languageMarket) {
        this.languageMarket = languageMarket;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
