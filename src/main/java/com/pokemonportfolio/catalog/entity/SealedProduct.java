package com.pokemonportfolio.catalog.entity;

import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.SealedProductType;
import com.pokemonportfolio.config.domain.VerificationStatus;
import com.pokemonportfolio.config.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "sealed_product")
public class SealedProduct extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private SealedProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_market", nullable = false)
    private LanguageMarket languageMarket;

    @Column(name = "set_name")
    private String setName;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "catalog_source", nullable = false)
    private CatalogSource catalogSource = CatalogSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    protected SealedProduct() {
    }

    public SealedProduct(
            String name,
            SealedProductType productType,
            LanguageMarket languageMarket,
            String setName,
            LocalDate releaseDate,
            String imageUrl,
            String notes) {
        this.name = name;
        this.productType = productType;
        this.languageMarket = languageMarket;
        this.setName = setName;
        this.releaseDate = releaseDate;
        this.imageUrl = imageUrl;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SealedProductType getProductType() {
        return productType;
    }

    public LanguageMarket getLanguageMarket() {
        return languageMarket;
    }

    public String getSetName() {
        return setName;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public CatalogSource getCatalogSource() {
        return catalogSource;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isActive() {
        return active;
    }
}
