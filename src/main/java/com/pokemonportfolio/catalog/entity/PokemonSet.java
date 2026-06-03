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
}

