package com.pokemonportfolio.grading.service;

import java.math.BigDecimal;

public class GradingAnalysisForm {

    private Long ownedItemId;
    private BigDecimal rawValueSgd;
    private BigDecimal psa8ValueSgd;
    private BigDecimal psa9ValueSgd;
    private BigDecimal psa10ValueSgd;
    private Long gradingFeeId;
    private Integer estimatedTurnaroundDays;
    private BigDecimal opportunityCostSgd = BigDecimal.ZERO;
    private String notes;

    public Long getOwnedItemId() {
        return ownedItemId;
    }

    public void setOwnedItemId(Long ownedItemId) {
        this.ownedItemId = ownedItemId;
    }

    public BigDecimal getRawValueSgd() {
        return rawValueSgd;
    }

    public void setRawValueSgd(BigDecimal rawValueSgd) {
        this.rawValueSgd = rawValueSgd;
    }

    public BigDecimal getPsa8ValueSgd() {
        return psa8ValueSgd;
    }

    public void setPsa8ValueSgd(BigDecimal psa8ValueSgd) {
        this.psa8ValueSgd = psa8ValueSgd;
    }

    public BigDecimal getPsa9ValueSgd() {
        return psa9ValueSgd;
    }

    public void setPsa9ValueSgd(BigDecimal psa9ValueSgd) {
        this.psa9ValueSgd = psa9ValueSgd;
    }

    public BigDecimal getPsa10ValueSgd() {
        return psa10ValueSgd;
    }

    public void setPsa10ValueSgd(BigDecimal psa10ValueSgd) {
        this.psa10ValueSgd = psa10ValueSgd;
    }

    public Long getGradingFeeId() {
        return gradingFeeId;
    }

    public void setGradingFeeId(Long gradingFeeId) {
        this.gradingFeeId = gradingFeeId;
    }

    public Integer getEstimatedTurnaroundDays() {
        return estimatedTurnaroundDays;
    }

    public void setEstimatedTurnaroundDays(Integer estimatedTurnaroundDays) {
        this.estimatedTurnaroundDays = estimatedTurnaroundDays;
    }

    public BigDecimal getOpportunityCostSgd() {
        return opportunityCostSgd;
    }

    public void setOpportunityCostSgd(BigDecimal opportunityCostSgd) {
        this.opportunityCostSgd = opportunityCostSgd;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
