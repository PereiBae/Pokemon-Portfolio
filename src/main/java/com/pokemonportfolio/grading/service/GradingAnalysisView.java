package com.pokemonportfolio.grading.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.GradingRecommendation;
import com.pokemonportfolio.grading.entity.GradingAnalysis;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record GradingAnalysisView(
        Long id,
        Long ownedItemId,
        String itemDisplayName,
        String imageSmallUrl,
        String setName,
        String cardNumber,
        String variantLabel,
        BigDecimal purchasePriceSgd,
        BigDecimal rawValueSgd,
        BigDecimal gradingFeeSgd,
        BigDecimal opportunityCostSgd,
        Integer estimatedTurnaroundDays,
        BigDecimal minimumProfitThresholdSgd,
        GradingRecommendation recommendation,
        String recommendationLabel,
        ConfidenceRating confidenceRating,
        String notes,
        OffsetDateTime createdAt,
        List<GradingScenarioView> scenarios) {

    public static GradingAnalysisView from(GradingAnalysis analysis, List<GradingScenarioView> scenarios) {
        var item = analysis.getOwnedItem();
        return new GradingAnalysisView(
                analysis.getId(),
                item.getId(),
                item.displayName(),
                item.imageSmallUrl(),
                item.setName(),
                item.assetNumber(),
                item.variantOrTypeLabel(),
                item.getPurchasePriceSgd(),
                analysis.getRawValueSgd(),
                analysis.getGradingFeeSgd(),
                analysis.getOpportunityCostSgd(),
                analysis.getEstimatedTurnaroundDays(),
                analysis.getMinimumProfitThresholdSgd(),
                analysis.getRecommendation(),
                analysis.getRecommendation().getLabel(),
                analysis.getConfidenceRating(),
                analysis.getNotes(),
                analysis.getCreatedAt(),
                scenarios);
    }
}
