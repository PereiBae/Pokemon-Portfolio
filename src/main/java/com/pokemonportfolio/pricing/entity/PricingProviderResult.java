package com.pokemonportfolio.pricing.entity;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.config.domain.AssetType;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.PricingResultType;
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
@Table(name = "pricing_provider_result")
public class PricingProviderResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType = AssetType.CARD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sealed_product_id")
    private SealedProduct sealedProduct;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_variant")
    private CardVariant cardVariant;

    @Column(name = "provider_name", nullable = false, length = 120)
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 80)
    private PricingResultType resultType;

    @Column(name = "source_market", length = 120)
    private String sourceMarket;

    @Column(name = "source_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal sourcePrice;

    @Column(name = "source_currency", nullable = false, length = 3)
    private String sourceCurrency;

    @Column(name = "exchange_rate_used", nullable = false, precision = 19, scale = 8)
    private BigDecimal exchangeRateUsed;

    @Column(name = "price_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceSgd;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_rating", nullable = false)
    private ConfidenceRating confidenceRating;

    @Column(name = "sample_size")
    private Integer sampleSize;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "source_updated_at")
    private OffsetDateTime sourceUpdatedAt;

    @Column(name = "provider_metadata", length = 2000)
    private String providerMetadata;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    protected PricingProviderResult() {
    }

    public PricingProviderResult(
            Card card,
            CardVariant cardVariant,
            String providerName,
            PricingResultType resultType,
            String sourceMarket,
            BigDecimal sourcePrice,
            String sourceCurrency,
            BigDecimal exchangeRateUsed,
            BigDecimal priceSgd,
            ConfidenceRating confidenceRating,
            Integer sampleSize,
            String sourceUrl,
            OffsetDateTime sourceUpdatedAt,
            String providerMetadata,
            OffsetDateTime capturedAt) {
        this.assetType = AssetType.CARD;
        this.card = card;
        this.cardVariant = cardVariant;
        this.providerName = providerName;
        this.resultType = resultType;
        this.sourceMarket = sourceMarket;
        this.sourcePrice = sourcePrice;
        this.sourceCurrency = sourceCurrency;
        this.exchangeRateUsed = exchangeRateUsed;
        this.priceSgd = priceSgd;
        this.confidenceRating = confidenceRating;
        this.sampleSize = sampleSize;
        this.sourceUrl = sourceUrl;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.providerMetadata = providerMetadata;
        this.capturedAt = capturedAt;
    }

    public Long getId() {
        return id;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public Card getCard() {
        return card;
    }

    public SealedProduct getSealedProduct() {
        return sealedProduct;
    }

    public CardVariant getCardVariant() {
        return cardVariant;
    }

    public String getProviderName() {
        return providerName;
    }

    public PricingResultType getResultType() {
        return resultType;
    }

    public String getSourceMarket() {
        return sourceMarket;
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

    public BigDecimal getPriceSgd() {
        return priceSgd;
    }

    public ConfidenceRating getConfidenceRating() {
        return confidenceRating;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public OffsetDateTime getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public String getProviderMetadata() {
        return providerMetadata;
    }

    public OffsetDateTime getCapturedAt() {
        return capturedAt;
    }
}
