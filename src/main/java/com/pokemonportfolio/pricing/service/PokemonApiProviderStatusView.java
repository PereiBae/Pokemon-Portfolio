package com.pokemonportfolio.pricing.service;

public record PokemonApiProviderStatusView(
        String providerName,
        boolean enabled,
        boolean apiKeyConfigured,
        String baseUrl) {
}
