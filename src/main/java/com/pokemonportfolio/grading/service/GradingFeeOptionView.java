package com.pokemonportfolio.grading.service;

import com.pokemonportfolio.config.domain.GradingCompany;
import com.pokemonportfolio.grading.entity.GradingFee;
import java.math.BigDecimal;

public record GradingFeeOptionView(
        Long id,
        GradingCompany gradingCompany,
        String companyLabel,
        String serviceLevelName,
        BigDecimal feeSgd,
        Integer estimatedTurnaroundDays,
        String displayLabel) {

    public static GradingFeeOptionView from(GradingFee fee) {
        return new GradingFeeOptionView(
                fee.getId(),
                fee.getGradingCompany(),
                fee.getGradingCompany().getLabel(),
                fee.getServiceLevelName(),
                fee.getFeeSgd(),
                fee.getEstimatedTurnaroundDays(),
                fee.getGradingCompany().getLabel()
                        + " - "
                        + fee.getServiceLevelName()
                        + " (SGD "
                        + fee.getFeeSgd()
                        + ", "
                        + fee.getEstimatedTurnaroundDays()
                        + " days)");
    }
}
