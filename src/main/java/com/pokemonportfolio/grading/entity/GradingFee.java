package com.pokemonportfolio.grading.entity;

import com.pokemonportfolio.config.domain.GradingCompany;
import com.pokemonportfolio.config.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "grading_fee")
public class GradingFee extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_company", nullable = false)
    private GradingCompany gradingCompany = GradingCompany.PSA;

    @Column(name = "service_level_name", nullable = false)
    private String serviceLevelName;

    @Column(name = "fee_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal feeSgd;

    @Column(name = "estimated_turnaround_days", nullable = false)
    private Integer estimatedTurnaroundDays;

    @Column(nullable = false)
    private boolean active = true;

    protected GradingFee() {
    }

    public GradingFee(
            GradingCompany gradingCompany,
            String serviceLevelName,
            BigDecimal feeSgd,
            Integer estimatedTurnaroundDays,
            boolean active) {
        this.gradingCompany = gradingCompany;
        this.serviceLevelName = serviceLevelName;
        this.feeSgd = feeSgd;
        this.estimatedTurnaroundDays = estimatedTurnaroundDays;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public GradingCompany getGradingCompany() {
        return gradingCompany;
    }

    public String getServiceLevelName() {
        return serviceLevelName;
    }

    public BigDecimal getFeeSgd() {
        return feeSgd;
    }

    public Integer getEstimatedTurnaroundDays() {
        return estimatedTurnaroundDays;
    }

    public boolean isActive() {
        return active;
    }
}
