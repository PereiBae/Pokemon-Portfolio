package com.pokemonportfolio.catalog.entity;

import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.VerificationStatus;
import com.pokemonportfolio.config.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "card")
public class Card extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pokemon_set_id", nullable = false)
    private PokemonSet pokemonSet;

    @Column(nullable = false)
    private String name;

    @Column(name = "card_number", nullable = false)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_market", nullable = false)
    private LanguageMarket languageMarket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardVariant variant;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "catalog_source", nullable = false)
    private CatalogSource catalogSource = CatalogSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Column(name = "external_card_id", length = 120)
    private String externalCardId;

    @Column(name = "external_image_url", length = 1000)
    private String externalImageUrl;

    @Column(name = "external_image_large_url", length = 1000)
    private String externalImageLargeUrl;

    @Column(name = "external_card_url", length = 1000)
    private String externalCardUrl;

    @Column(length = 120)
    private String rarity;

    @Column(name = "available_variant_codes", length = 500, nullable = false)
    private String availableVariantCodes = CardVariant.STANDARD.name();

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    protected Card() {
    }

    public Card(PokemonSet pokemonSet, String name, String cardNumber, LanguageMarket languageMarket, CardVariant variant) {
        this.pokemonSet = pokemonSet;
        this.name = name;
        this.cardNumber = cardNumber;
        this.languageMarket = languageMarket;
        this.variant = variant;
        this.availableVariantCodes = serializeVariants(List.of(variant));
    }

    public Long getId() {
        return id;
    }

    public PokemonSet getPokemonSet() {
        return pokemonSet;
    }

    public String getName() {
        return name;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public LanguageMarket getLanguageMarket() {
        return languageMarket;
    }

    public CardVariant getVariant() {
        return variant;
    }

    public boolean isActive() {
        return active;
    }

    public CatalogSource getCatalogSource() {
        return catalogSource;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public String getExternalCardId() {
        return externalCardId;
    }

    public String getExternalImageUrl() {
        return externalImageUrl;
    }

    public String getExternalImageSmallUrl() {
        return externalImageUrl;
    }

    public String getExternalImageLargeUrl() {
        return externalImageLargeUrl;
    }

    public String getExternalCardUrl() {
        return externalCardUrl;
    }

    public String getRarity() {
        return rarity;
    }

    public List<CardVariant> getAvailableVariants() {
        if (availableVariantCodes == null || availableVariantCodes.isBlank()) {
            return List.of(variant);
        }
        return Arrays.stream(availableVariantCodes.split(","))
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .map(CardVariant::valueOf)
                .distinct()
                .toList();
    }

    public CardVariant getDefaultOwnedVariant() {
        List<CardVariant> availableVariants = getAvailableVariants();
        if (availableVariants.isEmpty()) {
            return variant;
        }
        return availableVariants.getFirst();
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void markVerified(
            CatalogSource catalogSource,
            String externalCardId,
            String externalImageSmallUrl,
            String externalImageLargeUrl,
            String externalCardUrl,
            String rarity,
            List<CardVariant> availableVariants,
            OffsetDateTime lastSyncedAt) {
        this.catalogSource = catalogSource;
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.externalCardId = externalCardId;
        this.externalImageUrl = externalImageSmallUrl;
        this.externalImageLargeUrl = externalImageLargeUrl;
        this.externalCardUrl = externalCardUrl;
        this.rarity = rarity;
        this.availableVariantCodes = serializeVariants(availableVariants == null || availableVariants.isEmpty()
                ? List.of(CardVariant.STANDARD)
                : availableVariants);
        this.lastSyncedAt = lastSyncedAt;
    }

    public void moveToSet(PokemonSet pokemonSet) {
        this.pokemonSet = pokemonSet;
    }

    public String getDisplayName() {
        return name + " #" + cardNumber;
    }

    private String serializeVariants(List<CardVariant> variants) {
        return variants.stream()
                .distinct()
                .map(CardVariant::name)
                .collect(Collectors.joining(","));
    }
}
