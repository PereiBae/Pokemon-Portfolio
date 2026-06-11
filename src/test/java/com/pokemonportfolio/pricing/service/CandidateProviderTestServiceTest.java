package com.pokemonportfolio.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.provider.ExternalPricingProbeCardView;
import com.pokemonportfolio.pricing.provider.poketrace.PokeTracePricingProperties;
import com.pokemonportfolio.pricing.provider.poketrace.PokeTracePricingProvider;
import com.pokemonportfolio.pricing.provider.poketrace.PokeTracePricingProviderException;
import com.pokemonportfolio.pricing.provider.pokemonpricetracker.PokemonPriceTrackerPricingProperties;
import com.pokemonportfolio.pricing.provider.pokemonpricetracker.PokemonPriceTrackerProvider;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CandidateProviderTestServiceTest {

    @Test
    void pokeTraceDisabledProviderShowsFriendlyMessageWithoutCallingProvider() {
        PokeTracePricingProperties properties = pokeTraceProperties(false, "");
        PokeTracePricingProvider provider = mock(PokeTracePricingProvider.class);
        PokeTraceProviderTestService service = new PokeTraceProviderTestService(properties, provider);

        assertThatThrownBy(() -> service.test(form("test-card")))
                .isInstanceOf(PricingProviderProbeException.class)
                .hasMessage("PokeTrace provider is disabled. Set POKETRACE_PRICING_ENABLED=true.");
        verifyNoInteractions(provider);
    }

    @Test
    void pokemonPriceTrackerMissingKeyShowsFriendlyMessageWithoutCallingProvider() {
        PokemonPriceTrackerPricingProperties properties = pokemonPriceTrackerProperties(true, "");
        PokemonPriceTrackerProvider provider = mock(PokemonPriceTrackerProvider.class);
        PokemonPriceTrackerProviderTestService service = new PokemonPriceTrackerProviderTestService(properties, provider);

        assertThatThrownBy(() -> service.test(form("490294")))
                .isInstanceOf(PricingProviderProbeException.class)
                .hasMessage("PokemonPriceTracker API key is missing. Set POKEMON_PRICE_TRACKER_API_KEY.");
        verifyNoInteractions(provider);
    }

    @Test
    void successfulPokeTraceLookupReturnsDisplayOnlyResult() {
        PokeTracePricingProvider provider = mock(PokeTracePricingProvider.class);
        when(provider.providerName()).thenReturn("PokeTracePricingProvider");
        when(provider.fetchOneCard("test-card")).thenReturn(cardView("test-card"));
        PokeTraceProviderTestService service = new PokeTraceProviderTestService(
                pokeTraceProperties(true, "key"),
                provider);

        PricingProviderProbeResultView result = service.test(form("test-card"));

        assertThat(result.sourceProvider()).isEqualTo("PokeTracePricingProvider");
        assertThat(result.sourceUrl()).isEqualTo("https://example.test/cards/test-card");
        assertThat(result.card().rawNearMintPrice()).isEqualByComparingTo("88.00");
    }

    @Test
    void providerExceptionBecomesFriendlyProbeException() {
        PokeTracePricingProvider provider = mock(PokeTracePricingProvider.class);
        when(provider.fetchOneCard("test-card")).thenThrow(new PokeTracePricingProviderException("Quota exceeded."));
        PokeTraceProviderTestService service = new PokeTraceProviderTestService(
                pokeTraceProperties(true, "key"),
                provider);

        assertThatThrownBy(() -> service.test(form("test-card")))
                .isInstanceOf(PricingProviderProbeException.class)
                .hasMessage("PokeTrace test request failed. Quota exceeded.");
    }

    private PricingProviderProbeForm form(String searchOrId) {
        PricingProviderProbeForm form = new PricingProviderProbeForm();
        form.setSearchOrId(searchOrId);
        return form;
    }

    private PokeTracePricingProperties pokeTraceProperties(boolean enabled, String apiKey) {
        PokeTracePricingProperties properties = new PokeTracePricingProperties();
        properties.setEnabled(enabled);
        properties.setBaseUrl("https://api.poketrace.com/v1");
        properties.setApiKey(apiKey);
        return properties;
    }

    private PokemonPriceTrackerPricingProperties pokemonPriceTrackerProperties(boolean enabled, String apiKey) {
        PokemonPriceTrackerPricingProperties properties = new PokemonPriceTrackerPricingProperties();
        properties.setEnabled(enabled);
        properties.setBaseUrl("https://www.pokemonpricetracker.com/api/v2");
        properties.setApiKey(apiKey);
        return properties;
    }

    private ExternalPricingProbeCardView cardView(String id) {
        return new ExternalPricingProbeCardView(
                id,
                "Giratina VSTAR",
                "Crown Zenith",
                "GG69",
                "English",
                "Holofoil",
                null,
                "TCGPLAYER",
                "USD",
                new BigDecimal("88.00"),
                null,
                new BigDecimal("88.00"),
                null,
                5,
                null,
                null,
                null,
                null,
                "No PSA prices mapped.",
                null,
                "Mocked exact response",
                ConfidenceRating.LOW,
                "https://example.test/cards/" + id);
    }
}
