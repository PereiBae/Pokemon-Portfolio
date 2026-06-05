package com.pokemonportfolio.config.domain;

public enum GradingRecommendation {
    STRONG_GRADE("Strong Grade"),
    GRADE_ONLY_IF_CONFIDENT_PSA10("Grade Only If Confident PSA 10"),
    HOLD_RAW("Hold Raw"),
    DO_NOT_GRADE("Do Not Grade"),
    INSUFFICIENT_DATA("Insufficient Data");

    private final String label;

    GradingRecommendation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
