package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.pricing.provider.PricingProviderAdapter;
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
        return adapters.stream()
                .filter(PricingProviderAdapter::isEnabled)
                .findFirst()
                .map(adapter -> adapter.fetchCardPrice(card))
                .orElseThrow(() -> new IllegalStateException("No enabled pricing provider is available"));
    }
}

