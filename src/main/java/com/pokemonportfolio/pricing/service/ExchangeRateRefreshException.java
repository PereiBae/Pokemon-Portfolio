package com.pokemonportfolio.pricing.service;

public class ExchangeRateRefreshException extends RuntimeException {

    public ExchangeRateRefreshException(String message) {
        super(message);
    }

    public ExchangeRateRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}
