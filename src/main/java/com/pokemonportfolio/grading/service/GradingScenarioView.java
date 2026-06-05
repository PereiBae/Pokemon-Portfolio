package com.pokemonportfolio.grading.service;

import com.pokemonportfolio.config.domain.GradingRecommendation;
import com.pokemonportfolio.config.domain.GradingScenarioType;
import com.pokemonportfolio.config.domain.PsaGrade;
import com.pokemonportfolio.grading.entity.GradingScenario;
import java.math.BigDecimal;

public record GradingScenarioView(
        GradingScenarioType scenarioType,
        String scenarioLabel,
        PsaGrade assumedGrade,
        String assumedGradeLabel,
        BigDecimal expectedGradedValueSgd,
        BigDecimal totalCostSgd,
        BigDecimal expectedProfitSgd,
        BigDecimal roiPercentage,
        GradingRecommendation recommendation,
        String recommendationLabel,
        String warningMessage) {

    public static GradingScenarioView from(GradingScenario scenario) {
        return new GradingScenarioView(
                scenario.getScenarioType(),
                scenario.getScenarioType().getLabel(),
                scenario.getAssumedGrade(),
                scenario.getAssumedGrade().getLabel(),
                scenario.getExpectedGradedValueSgd(),
                scenario.getTotalCostSgd(),
                scenario.getExpectedProfitSgd(),
                scenario.getRoiPercentage(),
                scenario.getRecommendation(),
                scenario.getRecommendation().getLabel(),
                scenario.getWarningMessage());
    }

    public boolean profitIsNonNegative() {
        return expectedProfitSgd.compareTo(BigDecimal.ZERO) >= 0;
    }
}
