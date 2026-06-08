package com.pokemonportfolio.pricing.provider;

public interface ExchangeRateProvider {

    String providerName();

    boolean isEnabled();

    ExchangeRateQuote latestRate(String sourceCurrency, String targetCurrency);
}
