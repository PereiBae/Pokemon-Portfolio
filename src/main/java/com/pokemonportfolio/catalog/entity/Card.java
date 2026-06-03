package com.pokemonportfolio.catalog.entity;

import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.LanguageMarket;
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

    protected Card() {
    }

    public Card(PokemonSet pokemonSet, String name, String cardNumber, LanguageMarket languageMarket, CardVariant variant) {
        this.pokemonSet = pokemonSet;
        this.name = name;
        this.cardNumber = cardNumber;
        this.languageMarket = languageMarket;
        this.variant = variant;
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

    public String getDisplayName() {
        return name + " #" + cardNumber;
    }
}

