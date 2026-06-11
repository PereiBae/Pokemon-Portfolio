package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.pricing.provider.ExternalPricingProbeCardView;
import com.pokemonportfolio.pricing.provider.poketrace.PokeTracePricingProperties;
import com.pokemonportfolio.pricing.provider.poketrace.PokeTracePricingProvider;
import com.pokemonportfolio.pricing.provider.poketrace.PokeTracePricingProviderException;
import org.springframework.stereotype.Service;

@Service
public class PokeTraceProviderTestService {

    private static final String DISABLED_MESSAGE =
            "PokeTrace provider is disabled. Set POKETRACE_PRICING_ENABLED=true.";
    private static final String MISSING_KEY_MESSAGE =
            "PokeTrace API key is missing. Set POKETRACE_API_KEY.";

    private final PokeTracePricingProperties properties;
    private final PokeTracePricingProvider provider;

    public PokeTraceProviderTestService(
            PokeTracePricingProperties properties,
            PokeTracePricingProvider provider) {
        this.properties = properties;
        this.provider = provider;
    }

    public PricingProviderProbeStatusView status() {
        return new PricingProviderProbeStatusView(
                provider.providerName(),
                properties.isEnabled(),
                apiKeyConfigured(),
                properties.getBaseUrl(),
                "PokeTrace API Key");
    }

    public PricingProviderProbeResultView test(PricingProviderProbeForm form) {
        ensureUsable();
        String searchOrId = requiredSearchOrId(form.getSearchOrId());
        try {
            ExternalPricingProbeCardView card = provider.fetchOneCard(searchOrId);
            return new PricingProviderProbeResultView(provider.providerName(), card.sourceUrl(), card);
        } catch (PokeTracePricingProviderException ex) {
            throw new PricingProviderProbeException("PokeTrace test request failed. " + ex.getMessage());
        } catch (RuntimeException ex) {
            throw new PricingProviderProbeException("PokeTrace test request failed.");
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
