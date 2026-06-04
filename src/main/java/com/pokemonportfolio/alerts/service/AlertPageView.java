package com.pokemonportfolio.alerts.service;

import java.util.List;

public record AlertPageView(
        List<AlertView> activeAlerts,
        List<AlertView> historicalAlerts) {

    public int activeCount() {
        return activeAlerts.size();
    }
}
