package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.pricing.provider.ExternalPricingProbeCardView;
import com.pokemonportfolio.pricing.provider.pokemonpricetracker.PokemonPriceTrackerPricingProperties;
import com.pokemonportfolio.pricing.provider.pokemonpricetracker.PokemonPriceTrackerProvider;
import com.pokemonportfolio.pricing.provider.pokemonpricetracker.PokemonPriceTrackerProviderException;
import org.springframework.stereotype.Service;

@Service
public class PokemonPriceTrackerProviderTestService {

    private static final String DISABLED_MESSAGE =
            "PokemonPriceTracker provider is disabled. Set POKEMON_PRICE_TRACKER_ENABLED=true.";
    private static final String MISSING_KEY_MESSAGE =
            "PokemonPriceTracker API key is missing. Set POKEMON_PRICE_TRACKER_API_KEY.";

    private final PokemonPriceTrackerPricingProperties properties;
    private final PokemonPriceTrackerProvider provider;

    public PokemonPriceTrackerProviderTestService(
            PokemonPriceTrackerPricingProperties properties,
            PokemonPriceTrackerProvider provider) {
        this.properties = properties;
        this.provider = provider;
    }

    public PricingProviderProbeStatusView status() {
        return new PricingProviderProbeStatusView(
                provider.providerName(),
                properties.isEnabled(),
                apiKeyConfigured(),
                properties.getBaseUrl(),
                "PokemonPriceTracker API Key");
    }

    public PricingProviderProbeResultView test(PricingProviderProbeForm form) {
        ensureUsable();
        String searchOrId = requiredSearchOrId(form.getSearchOrId());
        try {
            ExternalPricingProbeCardView card = provider.fetchOneCard(searchOrId);
            return new PricingProviderProbeResultView(provider.providerName(), card.sourceUrl(), card);
        } catch (PokemonPriceTrackerProviderException ex) {
            throw new PricingProviderProbeException("PokemonPriceTracker test request failed. " + ex.getMessage());
        } catch (RuntimeException ex) {
            throw new PricingProviderProbeException("PokemonPriceTracker test request failed.");
        }
    }

    private void ensureUsable() {
        if (!properties.isEnabled()) {
            throw new PricingProviderProbeException(DISABLED_MESSAGE);
        }
        if (!apiKeyConfigured()) {
            throw new PricingProviderProbeException(MISSING_KEY_MESSAGE);
        }
    }

    private boolean apiKeyConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    private String requiredSearchOrId(String searchOrId) {
        if (searchOrId == null || searchOrId.isBlank()) {
            throw new PricingProviderProbeException("Card id or search text is required.");
        }
        return searchOrId.trim();
    }
}
