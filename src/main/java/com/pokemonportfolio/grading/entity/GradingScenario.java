package com.pokemonportfolio.grading.entity;

import com.pokemonportfolio.config.domain.GradingRecommendation;
import com.pokemonportfolio.config.domain.GradingScenarioType;
import com.pokemonportfolio.config.domain.PsaGrade;
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
import java.math.BigDecimal;

@Entity
@Table(name = "grading_scenario")
public class GradingScenario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grading_analysis_id", nullable = false)
    private GradingAnalysis gradingAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type", nullable = false)
    private GradingScenarioType scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(name = "assumed_grade", nullable = false)
    private PsaGrade assumedGrade;

    @Column(name = "expected_graded_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedGradedValueSgd;

    @Column(name = "total_cost_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCostSgd;

    @Column(name = "expected_profit_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedProfitSgd;

    @Column(name = "roi_percentage", nullable = false, precision = 12, scale = 4)
    private BigDecimal roiPercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradingRecommendation recommendation;

    @Column(name = "warning_message", length = 1000)
    private String warningMessage;

    protected GradingScenario() {
    }

    public GradingScenario(
            GradingAnalysis gradingAnalysis,
            GradingScenarioType scenarioType,
            PsaGrade assumedGrade,
            BigDecimal expectedGradedValueSgd,
            BigDecimal totalCostSgd,
            BigDecimal expectedProfitSgd,
            BigDecimal roiPercentage,
            GradingRecommendation recommendation,
            String warningMessage) {
        this.gradingAnalysis = gradingAnalysis;
        this.scenarioType = scenarioType;
        this.assumedGrade = assumedGrade;
        this.expectedGradedValueSgd = expectedGradedValueSgd;
        this.totalCostSgd = totalCostSgd;
        this.expectedProfitSgd = expectedProfitSgd;
        this.roiPercentage = roiPercentage;
        this.recommendation = recommendation;
        this.warningMessage = warningMessage;
    }

    public Long getId() {
        return id;
    }

    public GradingAnalysis getGradingAnalysis() {
        return gradingAnalysis;
    }

    public GradingScenarioType getScenarioType() {
        return scenarioType;
    }

    public PsaGrade getAssumedGrade() {
        return assumedGrade;
    }

    public BigDecimal getExpectedGradedValueSgd() {
        return expectedGradedValueSgd;
    }

    public BigDecimal getTotalCostSgd() {
        return totalCostSgd;
    }

    public BigDecimal getExpectedProfitSgd() {
        return expectedProfitSgd;
    }

    public BigDecimal getRoiPercentage() {
        return roiPercentage;
    }

    public GradingRecommendation getRecommendation() {
        return recommendation;
    }

    public String getWarningMessage() {
        return warningMessage;
    }
}
