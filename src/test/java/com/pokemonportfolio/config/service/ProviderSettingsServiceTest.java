package com.pokemonportfolio.config.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProviderSettingsServiceTest {

    @Autowired
    private ProviderSettingsService providerSettingsService;

    @Test
    void mockAndManualProvidersAreEnabledByDefaultAndRealProvidersAreDisabled() {
        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.MOCK_PROVIDER)).isTrue();
        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.MANUAL_PROVIDER)).isTrue();
        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.TCGPLAYER_PROVIDER)).isFalse();
        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.EBAY_PROVIDER)).isFalse();
        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.PRICECHARTING_PROVIDER)).isFalse();
        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.POKETRACE_PROVIDER)).isFalse();
        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.POKEMON_PRICE_TRACKER_PROVIDER)).isFalse();
    }

    @Test
    void togglesProviderEnablement() {
        providerSettingsService.setEnabled(ProviderSettingsService.MOCK_PROVIDER, false);

        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.MOCK_PROVIDER)).isFalse();

        providerSettingsService.setEnabled(ProviderSettingsService.MOCK_PROVIDER, true);

        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.MOCK_PROVIDER)).isTrue();
    }

    @Test
    void togglesCandidateProviderEnablement() {
        providerSettingsService.setEnabled(ProviderSettingsService.POKETRACE_PROVIDER, true);
        providerSettingsService.setEnabled(ProviderSettingsService.POKEMON_PRICE_TRACKER_PROVIDER, true);

        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.POKETRACE_PROVIDER)).isTrue();
        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.POKEMON_PRICE_TRACKER_PROVIDER)).isTrue();
    }
}
