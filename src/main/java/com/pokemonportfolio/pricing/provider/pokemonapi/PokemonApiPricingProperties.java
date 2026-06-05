package com.pokemonportfolio.pricing.provider.pokemonapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.pricing.providers.pokemon-api")
public class PokemonApiPricingProperties {

    private boolean enabled;
    private String baseUrl = "https://pokemon-tcg-api.p.rapidapi.com";
    private String rapidApiKey = "";

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

    public String getRapidApiKey() {
        return rapidApiKey;
    }

    public void setRapidApiKey(String rapidApiKey) {
        this.rapidApiKey = rapidApiKey;
    }
}
