package com.pokemonportfolio.grading.entity;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.GradingRecommendation;
import com.pokemonportfolio.config.entity.AuditableEntity;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
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

@Entity
@Table(name = "grading_analysis")
public class GradingAnalysis extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owned_item_id", nullable = false)
    private OwnedItem ownedItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_fee_id")
    private GradingFee gradingFee;

    @Column(name = "raw_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal rawValueSgd;

    @Column(name = "psa8_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal psa8ValueSgd;

    @Column(name = "psa9_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal psa9ValueSgd;

    @Column(name = "psa10_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal psa10ValueSgd;

    @Column(name = "grading_fee_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal gradingFeeSgd;

    @Column(name = "estimated_turnaround_days", nullable = false)
    private Integer estimatedTurnaroundDays;

    @Column(name = "opportunity_cost_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal opportunityCostSgd;

    @Column(name = "minimum_profit_threshold_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumProfitThresholdSgd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradingRecommendation recommendation;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_rating", nullable = false)
    private ConfidenceRating confidenceRating;

    @Column(length = 1000)
    private String notes;

    protected GradingAnalysis() {
    }

    public GradingAnalysis(
            AppUser owner,
            OwnedItem ownedItem,
            GradingFee gradingFee,
            BigDecimal rawValueSgd,
            BigDecimal psa8ValueSgd,
            BigDecimal psa9ValueSgd,
            BigDecimal psa10ValueSgd,
            BigDecimal gradingFeeSgd,
            Integer estimatedTurnaroundDays,
            BigDecimal opportunityCostSgd,
            BigDecimal minimumProfitThresholdSgd,
            GradingRecommendation recommendation,
            ConfidenceRating confidenceRating,
            String notes) {
        this.owner = owner;
        this.ownedItem = ownedItem;
        this.gradingFee = gradingFee;
        this.rawValueSgd = rawValueSgd;
        this.psa8ValueSgd = psa8ValueSgd;
        this.psa9ValueSgd = psa9ValueSgd;
        this.psa10ValueSgd = psa10ValueSgd;
        this.gradingFeeSgd = gradingFeeSgd;
        this.estimatedTurnaroundDays = estimatedTurnaroundDays;
        this.opportunityCostSgd = opportunityCostSgd;
        this.minimumProfitThresholdSgd = minimumProfitThresholdSgd;
        this.recommendation = recommendation;
        this.confidenceRating = confidenceRating;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public OwnedItem getOwnedItem() {
        return ownedItem;
    }

    public GradingFee getGradingFee() {
        return gradingFee;
    }

    public BigDecimal getRawValueSgd() {
        return rawValueSgd;
    }

    public BigDecimal getPsa8ValueSgd() {
        return psa8ValueSgd;
    }

    public BigDecimal getPsa9ValueSgd() {
        return psa9ValueSgd;
    }

    public BigDecimal getPsa10ValueSgd() {
        return psa10ValueSgd;
    }

    public BigDecimal getGradingFeeSgd() {
        return gradingFeeSgd;
    }

    public Integer getEstimatedTurnaroundDays() {
        return estimatedTurnaroundDays;
    }

    public BigDecimal getOpportunityCostSgd() {
        return opportunityCostSgd;
    }

    public BigDecimal getMinimumProfitThresholdSgd() {
        return minimumProfitThresholdSgd;
    }

    public GradingRecommendation getRecommendation() {
        return recommendation;
    }

    public ConfidenceRating getConfidenceRating() {
        return confidenceRating;
    }

    public String getNotes() {
        return notes;
    }
}
