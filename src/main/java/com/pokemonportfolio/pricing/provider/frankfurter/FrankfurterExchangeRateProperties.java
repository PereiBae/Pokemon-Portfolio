package com.pokemonportfolio.pricing.provider.frankfurter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.exchange-rates.providers.frankfurter")
public class FrankfurterExchangeRateProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.frankfurter.dev";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
