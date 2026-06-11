package com.pokemonportfolio.pricing.provider.pokemonpricetracker;

public class PokemonPriceTrackerProviderException extends RuntimeException {

    public PokemonPriceTrackerProviderException(String message) {
        super(message);
    }

    public PokemonPriceTrackerProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
