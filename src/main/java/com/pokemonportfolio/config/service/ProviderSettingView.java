package com.pokemonportfolio.config.service;

public record ProviderSettingView(
        String key,
        String label,
        boolean enabled,
        boolean realProvider,
        boolean toggleable,
        String statusLabel) {
}
