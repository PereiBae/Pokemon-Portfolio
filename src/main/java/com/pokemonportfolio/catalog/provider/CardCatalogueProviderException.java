package com.pokemonportfolio.catalog.provider;

public class CardCatalogueProviderException extends RuntimeException {

    public CardCatalogueProviderException(String message) {
        super(message);
    }

    public CardCatalogueProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
