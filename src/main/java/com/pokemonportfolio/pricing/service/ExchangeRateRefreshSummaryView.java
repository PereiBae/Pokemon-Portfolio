package com.pokemonportfolio.pricing.service;

import java.util.List;

public record ExchangeRateRefreshSummaryView(
        int requestedCount,
        int storedCount,
        int reusedCount,
        int failedCount,
        List<String> messages) {

    public ExchangeRateRefreshSummaryView {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public boolean hasFailures() {
        return failedCount > 0;
    }

    public boolean hasSuccesses() {
        return storedCount > 0 || reusedCount > 0;
    }
}
