package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.pricing.provider.PricingProviderAdapter;
import com.pokemonportfolio.pricing.provider.PricingProviderCardPrices;
import com.pokemonportfolio.pricing.provider.PricingProviderException;
import com.pokemonportfolio.pricing.provider.PricingProviderPrice;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PricingProviderService {

    private final List<PricingProviderAdapter> adapters;

    public PricingProviderService(List<PricingProviderAdapter> adapters) {
        this.adapters = adapters;
    }

    public PricingProviderPrice fetchBestCardPrice(Card card) {
        PricingProviderException lastProviderException = null;
        for (PricingProviderAdapter adapter : adapters.stream().filter(PricingProviderAdapter::isEnabled).toList()) {
            try {
                return adapter.fetchCardPrice(card);
            } catch (PricingProviderException ex) {
                lastProviderException = ex;
            }
        }
        if (lastProviderException != null) {
            throw lastProviderException;
        }
        throw new IllegalStateException("No enabled pricing provider is available");
    }

    public PricingProviderCardPrices fetchCardPrices(String providerName, Card card, CardVariant variant) {
        PricingProviderAdapter adapter = requireProvider(providerName);
        if (!adapter.isEnabled()) {
            throw new IllegalStateException(adapter.unavailableMessage());
        }
        return adapter.fetchCardPrices(card, variant);
    }

    public boolean isProviderEnabled(String providerName) {
        return requireProvider(providerName).isEnabled();
    }

    public String unavailableMessage(String providerName) {
        return requireProvider(providerName).unavailableMessage();
    }

    private PricingProviderAdapter requireProvider(String providerName) {
        return adapters.stream()
                .filter(adapter -> adapter.providerName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pricing provider not found: " + providerName));
    }
}
