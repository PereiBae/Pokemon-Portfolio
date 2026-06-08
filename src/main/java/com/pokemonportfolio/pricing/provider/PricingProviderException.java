package com.pokemonportfolio.pricing.provider;

public class PricingProviderException extends RuntimeException {

    private final PricingProviderSkipReason skipReason;
    private final String sourceCurrency;

    public PricingProviderException(PricingProviderSkipReason skipReason, String message) {
        super(message);
        this.skipReason = skipReason;
        this.sourceCurrency = null;
    }

    public PricingProviderException(PricingProviderSkipReason skipReason, String message, String sourceCurrency) {
        super(message);
        this.skipReason = skipReason;
        this.sourceCurrency = sourceCurrency;
    }

    public PricingProviderException(PricingProviderSkipReason skipReason, String message, Throwable cause) {
        super(message, cause);
        this.skipReason = skipReason;
        this.sourceCurrency = null;
    }

    public PricingProviderSkipReason getSkipReason() {
        return skipReason;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }
}
