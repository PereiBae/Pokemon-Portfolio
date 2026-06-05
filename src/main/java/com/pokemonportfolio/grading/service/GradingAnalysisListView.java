package com.pokemonportfolio.grading.service;

import com.pokemonportfolio.config.domain.GradingRecommendation;
import com.pokemonportfolio.grading.entity.GradingAnalysis;
import java.time.OffsetDateTime;

public record GradingAnalysisListView(
        Long id,
        String itemDisplayName,
        GradingRecommendation recommendation,
        String recommendationLabel,
        OffsetDateTime createdAt) {

    public static GradingAnalysisListView from(GradingAnalysis analysis) {
        return new GradingAnalysisListView(
                analysis.getId(),
                analysis.getOwnedItem().displayName(),
                analysis.getRecommendation(),
                analysis.getRecommendation().getLabel(),
                analysis.getCreatedAt());
    }
}
