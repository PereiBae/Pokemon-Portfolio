package com.pokemonportfolio.config.service;

import com.pokemonportfolio.pricing.provider.PricingProviderProperties;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProviderSettingsService {

    public static final String MOCK_PROVIDER = "mock";
    public static final String MANUAL_PROVIDER = "manual";
    public static final String TCGPLAYER_PROVIDER = "tcgplayer";
    public static final String EBAY_PROVIDER = "ebay";
    public static final String PRICECHARTING_PROVIDER = "pricecharting";

    private final PricingProviderProperties properties;

    public ProviderSettingsService(PricingProviderProperties properties) {
        this.properties = properties;
    }

    public List<ProviderSettingView> providerSettings() {
        return List.of(
                view(MOCK_PROVIDER, "Mock Pricing Provider", properties.isMockEnabled(), false, true),
                view(MANUAL_PROVIDER, "Manual Price Entry", properties.isManualEntryEnabled(), false, true),
                view(TCGPLAYER_PROVIDER, "TCGPlayer", properties.isTcgPlayerEnabled(), true, true),
                view(EBAY_PROVIDER, "eBay", properties.isEbayEnabled(), true, true),
                view(PRICECHARTING_PROVIDER, "PriceCharting", properties.isPriceChartingEnabled(), true, true));
    }

    public boolean isEnabled(String providerKey) {
        return switch (normalize(providerKey)) {
            case MOCK_PROVIDER -> properties.isMockEnabled();
            case MANUAL_PROVIDER -> properties.isManualEntryEnabled();
            case TCGPLAYER_PROVIDER -> properties.isTcgPlayerEnabled();
            case EBAY_PROVIDER -> properties.isEbayEnabled();
            case PRICECHARTING_PROVIDER -> properties.isPriceChartingEnabled();
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
