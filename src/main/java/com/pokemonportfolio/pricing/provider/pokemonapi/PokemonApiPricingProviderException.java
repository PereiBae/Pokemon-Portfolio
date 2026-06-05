package com.pokemonportfolio.pricing.provider.pokemonapi;

public class PokemonApiPricingProviderException extends RuntimeException {

    public PokemonApiPricingProviderException(String message) {
        super(message);
    }

    public PokemonApiPricingProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
