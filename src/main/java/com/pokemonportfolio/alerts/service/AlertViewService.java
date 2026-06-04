package com.pokemonportfolio.alerts.service;

import com.pokemonportfolio.alerts.entity.Alert;
import com.pokemonportfolio.alerts.repository.AlertRepository;
import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.AlertStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertViewService {

    private final AlertRepository alertRepository;

    public AlertViewService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public AlertPageView pageFor(AppUser owner) {
        List<AlertView> activeAlerts = alertRepository
                .findByOwnerAndStatusOrderByTriggeredAtDescIdDesc(owner, AlertStatus.ACTIVE)
                .stream()
                .map(this::toView)
                .toList();
        List<AlertView> historicalAlerts = alertRepository
                .findByOwnerOrderByTriggeredAtDescIdDesc(owner)
                .stream()
                .filter(alert -> alert.getStatus() != AlertStatus.ACTIVE)
                .map(this::toView)
                .toList();
        return new AlertPageView(activeAlerts, historicalAlerts);
    }

    @Transactional(readOnly = true)
    public long activeCount(AppUser owner) {
        return alertRepository.countByOwnerAndStatus(owner, AlertStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<AlertView> latestActiveAlerts(AppUser owner, int limit) {
        return alertRepository.findByOwnerAndStatusOrderByTriggeredAtDescIdDesc(owner, AlertStatus.ACTIVE)
                .stream()
                .limit(limit)
                .map(this::toView)
                .toList();
    }

    @Transactional
    public void dismissAlert(AppUser owner, Long alertId) {
        Alert alert = alertRepository.findByIdAndOwner(alertId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.dismiss(OffsetDateTime.now());
        alertRepository.save(alert);
    }

    @Transactional
    public void dismissActiveAlertsForOwnedItem(AppUser owner, Long ownedItemId) {
        alertRepository.findByOwnerAndOwnedItem_IdAndStatus(owner, ownedItemId, AlertStatus.ACTIVE)
                .forEach(alert -> alert.dismiss(OffsetDateTime.now()));
    }

    private AlertView toView(Alert alert) {
        return new AlertView(
                alert.getId(),
                alert.getItemDisplayName(),
                alert.getOwnedItem().imageSmallUrl(),
                alert.getPurchasePriceSgd(),
                alert.getCurrentMarketValueSgd(),
                alert.getGainAmountSgd(),
                alert.getGainPercentage(),
                alert.getConfidenceRating(),
                alert.getStatus(),
                alert.getTriggeredAt(),
                alert.getDismissedAt());
    }
}
