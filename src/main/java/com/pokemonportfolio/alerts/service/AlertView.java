package com.pokemonportfolio.alerts.service;

import com.pokemonportfolio.config.domain.AlertStatus;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AlertView(
        Long id,
        String itemDisplayName,
        String imageSmallUrl,
        BigDecimal purchasePriceSgd,
        BigDecimal currentMarketValueSgd,
        BigDecimal gainAmountSgd,
        BigDecimal gainPercentage,
        ConfidenceRating confidenceRating,
        AlertStatus status,
        OffsetDateTime triggeredAt,
        OffsetDateTime dismissedAt) {

    public boolean isActive() {
        return status == AlertStatus.ACTIVE || status == AlertStatus.NEW;
    }

    public String statusLabel() {
        return status.getLabel();
    }
}
