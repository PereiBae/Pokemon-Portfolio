package com.pokemonportfolio.config.service;

import com.pokemonportfolio.pricing.provider.PricingProviderProperties;
import com.pokemonportfolio.pricing.provider.poketrace.PokeTracePricingProperties;
import com.pokemonportfolio.pricing.provider.pokemonpricetracker.PokemonPriceTrackerPricingProperties;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProviderSettingsService {

    public static final String MOCK_PROVIDER = "mock";
    public static final String MANUAL_PROVIDER = "manual";
    public static final String TCGPLAYER_PROVIDER = "tcgplayer";
    public static final String EBAY_PROVIDER = "ebay";
    public static final String PRICECHARTING_PROVIDER = "pricecharting";
    public static final String POKETRACE_PROVIDER = "poketrace";
    public static final String POKEMON_PRICE_TRACKER_PROVIDER = "pokemon-price-tracker";

    private final PricingProviderProperties properties;
    private final PokeTracePricingProperties pokeTraceProperties;
    private final PokemonPriceTrackerPricingProperties pokemonPriceTrackerProperties;

    public ProviderSettingsService(
            PricingProviderProperties properties,
            PokeTracePricingProperties pokeTraceProperties,
            PokemonPriceTrackerPricingProperties pokemonPriceTrackerProperties) {
        this.properties = properties;
        this.pokeTraceProperties = pokeTraceProperties;
        this.pokemonPriceTrackerProperties = pokemonPriceTrackerProperties;
    }

    public List<ProviderSettingView> providerSettings() {
        return List.of(
                view(MOCK_PROVIDER, "Mock Pricing Provider", properties.isMockEnabled(), false, true),
                view(MANUAL_PROVIDER, "Manual Price Entry", properties.isManualEntryEnabled(), false, true),
                view(TCGPLAYER_PROVIDER, "TCGPlayer", properties.isTcgPlayerEnabled(), true, true),
                view(EBAY_PROVIDER, "eBay", properties.isEbayEnabled(), true, true),
                view(PRICECHARTING_PROVIDER, "PriceCharting", properties.isPriceChartingEnabled(), true, true),
                view(POKETRACE_PROVIDER, "PokeTrace", pokeTraceProperties.isEnabled(), true, true),
                view(POKEMON_PRICE_TRACKER_PROVIDER, "PokemonPriceTracker", pokemonPriceTrackerProperties.isEnabled(), true, true));
    }

    public boolean isEnabled(String providerKey) {
        return switch (normalize(providerKey)) {
            case MOCK_PROVIDER -> properties.isMockEnabled();
            case MANUAL_PROVIDER -> properties.isManualEntryEnabled();
            case TCGPLAYER_PROVIDER -> properties.isTcgPlayerEnabled();
            case EBAY_PROVIDER -> properties.isEbayEnabled();
            case PRICECHARTING_PROVIDER -> properties.isPriceChartingEnabled();
            case POKETRACE_PROVIDER -> pokeTraceProperties.isEnabled();
            case POKEMON_PRICE_TRACKER_PROVIDER -> pokemonPriceTrackerProperties.isEnabled();
            default -> throw new IllegalArgumentException("Unknown provider");
        };
    }

    public void setEnabled(String providerKey, boolean enabled) {
        switch (normalize(providerKey)) {
            case MOCK_PROVIDER -> properties.setMockEnabled(enabled);
            case MANUAL_PROVIDER -> properties.setManualEntryEnabled(enabled);
            case TCGPLAYER_PROVIDER -> properties.setTcgPlayerEnabled(enabled);
            case EBAY_PROVIDER -> properties.setEbayEnabled(enabled);
            case PRICECHARTING_PROVIDER -> properties.setPriceChartingEnabled(enabled);
            case POKETRACE_PROVIDER -> pokeTraceProperties.setEnabled(enabled);
            case POKEMON_PRICE_TRACKER_PROVIDER -> pokemonPriceTrackerProperties.setEnabled(enabled);
            default -> throw new IllegalArgumentException("Unknown provider");
        }
    }

    private ProviderSettingView view(
            String key,
            String label,
            boolean enabled,
            boolean realProvider,
            boolean toggleable) {
        String status = enabled ? "Enabled" : "Disabled";
        if (realProvider && !enabled) {
            status = "Disabled by default";
        }
        return new ProviderSettingView(key, label, enabled, realProvider, toggleable, status);
    }

    private String normalize(String providerKey) {
        return providerKey == null ? "" : providerKey.trim().toLowerCase();
    }
}
