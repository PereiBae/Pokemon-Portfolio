package com.pokemonportfolio.pricing.entity;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exchange_rate_snapshot")
public class ExchangeRateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_currency", nullable = false, length = 3)
    private String sourceCurrency;

    @Column(name = "target_currency", nullable = false, length = 3)
    private String targetCurrency;

    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    @Column(name = "rate_source", nullable = false)
    private String rateSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_rating", nullable = false)
    private ConfidenceRating confidenceRating;

    @Column(name = "effective_at", nullable = false)
    private OffsetDateTime effectiveAt;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    protected ExchangeRateSnapshot() {
    }

    public ExchangeRateSnapshot(
            String sourceCurrency,
            String targetCurrency,
            BigDecimal exchangeRate,
            String rateSource,
            ConfidenceRating confidenceRating,
            OffsetDateTime effectiveAt,
            OffsetDateTime fetchedAt) {
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.exchangeRate = exchangeRate;
        this.rateSource = rateSource;
        this.confidenceRating = confidenceRating;
        this.effectiveAt = effectiveAt;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public String getRateSource() {
        return rateSource;
    }

    public ConfidenceRating getConfidenceRating() {
        return confidenceRating;
    }

    public OffsetDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }
}
