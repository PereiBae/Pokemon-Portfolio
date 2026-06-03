package com.pokemonportfolio.pricing.entity;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.ConfidenceRating;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "price_snapshot")
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "source_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal sourcePrice;

    @Column(name = "source_currency", nullable = false, length = 3)
    private String sourceCurrency;

    @Column(name = "exchange_rate_used", nullable = false, precision = 19, scale = 8)
    private BigDecimal exchangeRateUsed;

    @Column(name = "market_price_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal marketPriceSgd;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_rating", nullable = false)
    private ConfidenceRating confidenceRating;

    @Column(nullable = false, length = 1000)
    private String explanation;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    protected PriceSnapshot() {
    }

    public PriceSnapshot(
            Card card,
            String providerName,
            BigDecimal sourcePrice,
            String sourceCurrency,
            BigDecimal exchangeRateUsed,
            BigDecimal marketPriceSgd,
            ConfidenceRating confidenceRating,
            String explanation,
            OffsetDateTime calculatedAt) {
        this.card = card;
        this.providerName = providerName;
        this.sourcePrice = sourcePrice;
        this.sourceCurrency = sourceCurrency;
        this.exchangeRateUsed = exchangeRateUsed;
        this.marketPriceSgd = marketPriceSgd;
        this.confidenceRating = confidenceRating;
        this.explanation = explanation;
        this.calculatedAt = calculatedAt;
    }

    public Long getId() {
        return id;
    }

    public Card getCard() {
        return card;
    }

    public String getProviderName() {
        return providerName;
    }

    public BigDecimal getSourcePrice() {
        return sourcePrice;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public BigDecimal getExchangeRateUsed() {
        return exchangeRateUsed;
    }

    public BigDecimal getMarketPriceSgd() {
        return marketPriceSgd;
    }

    public ConfidenceRating getConfidenceRating() {
        return confidenceRating;
    }

    public String getExplanation() {
        return explanation;
    }

    public OffsetDateTime getCalculatedAt() {
        return calculatedAt;
    }
}

