package com.pokemonportfolio.catalog.entity;

import com.pokemonportfolio.config.domain.LanguageMarket;
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
import java.time.OffsetDateTime;

@Entity
@Table(name = "pokemon_set")
public class PokemonSet extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_market", nullable = false)
    private LanguageMarket languageMarket;

    @Column(name = "external_set_id", length = 120)
    private String externalSetId;

    @Column(length = 120)
    private String series;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    protected PokemonSet() {
    }

    public PokemonSet(String name, LanguageMarket languageMarket) {
        this.name = name;
        this.languageMarket = languageMarket;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LanguageMarket getLanguageMarket() {
        return languageMarket;
    }

    public String getExternalSetId() {
        return externalSetId;
    }

    public String getSeries() {
        return series;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void markOfficial(String externalSetId, String series, LocalDate releaseDate, OffsetDateTime lastSyncedAt) {
        this.externalSetId = externalSetId;
        this.series = series;
        this.releaseDate = releaseDate;
        this.lastSyncedAt = lastSyncedAt;
    }
}
