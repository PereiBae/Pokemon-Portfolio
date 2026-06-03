package com.pokemonportfolio.pricing.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.pricing.providers")
public class PricingProviderProperties {

    private boolean mockEnabled = true;
    private boolean manualEntryEnabled = true;
    private boolean tcgPlayerEnabled;
    private boolean ebayEnabled;
    private boolean priceChartingEnabled;

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public boolean isManualEntryEnabled() {
        return manualEntryEnabled;
    }

    public void setManualEntryEnabled(boolean manualEntryEnabled) {
        this.manualEntryEnabled = manualEntryEnabled;
    }

    public boolean isTcgPlayerEnabled() {
        return tcgPlayerEnabled;
    }

    public void setTcgPlayerEnabled(boolean tcgPlayerEnabled) {
        this.tcgPlayerEnabled = tcgPlayerEnabled;
    }

    public boolean isEbayEnabled() {
        return ebayEnabled;
    }

    public void setEbayEnabled(boolean ebayEnabled) {
        this.ebayEnabled = ebayEnabled;
    }

    public boolean isPriceChartingEnabled() {
        return priceChartingEnabled;
    }

    public void setPriceChartingEnabled(boolean priceChartingEnabled) {
        this.priceChartingEnabled = priceChartingEnabled;
    }
}
