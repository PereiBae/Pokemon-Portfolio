package com.pokemonportfolio.pricing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.provider.ExternalPricingGradedPriceView;
import com.pokemonportfolio.pricing.provider.ExternalPricingProbeCardView;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import com.pokemonportfolio.pricing.service.PokeTraceProviderTestService;
import com.pokemonportfolio.pricing.service.PokemonPriceTrackerProviderTestService;
import com.pokemonportfolio.pricing.service.PricingProviderComparisonForm;
import com.pokemonportfolio.pricing.service.PricingProviderComparisonPageView;
import com.pokemonportfolio.pricing.service.PricingProviderComparisonRowView;
import com.pokemonportfolio.pricing.service.PricingProviderComparisonService;
import com.pokemonportfolio.pricing.service.PricingProviderProbeException;
import com.pokemonportfolio.pricing.service.PricingProviderProbeResultView;
import com.pokemonportfolio.pricing.service.PricingProviderProbeStatusView;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidatePricingProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @MockBean
    private PokeTraceProviderTestService pokeTraceService;

    @MockBean
    private PokemonPriceTrackerProviderTestService pokemonPriceTrackerService;

    @MockBean
    private PricingProviderComparisonService comparisonService;

    @BeforeEach
    void setUp() {
        when(pokeTraceService.status()).thenReturn(new PricingProviderProbeStatusView(
                "PokeTracePricingProvider",
                true,
                true,
                "https://api.poketrace.com/v1",
                "PokeTrace API Key"));
        when(pokemonPriceTrackerService.status()).thenReturn(new PricingProviderProbeStatusView(
                "PokemonPriceTrackerProvider",
                true,
                true,
                "https://www.pokemonpricetracker.com/api/v2",
                "PokemonPriceTracker API Key"));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void pokeTraceTestPageRendersForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/settings/providers/poketrace/test"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PokeTrace Provider Test")))
                .andExpect(content().string(containsString("Run PokeTrace Test")))
                .andExpect(content().string(containsString("Configured")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void pokeTraceDisabledAndMissingKeyMessagesAreFriendly() throws Exception {
        when(pokeTraceService.status()).thenReturn(new PricingProviderProbeStatusView(
                "PokeTracePricingProvider",
                false,
                false,
                "https://api.poketrace.com/v1",
                "PokeTrace API Key"));
        when(pokeTraceService.test(any())).thenThrow(new PricingProviderProbeException(
                "PokeTrace provider is disabled. Set POKETRACE_PRICING_ENABLED=true."));

        mockMvc.perform(post("/settings/providers/poketrace/test")
                        .with(csrf())
                        .param("searchOrId", "019bff77-befa-771d-bab0-f5909f0a78c9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "PokeTrace provider is disabled. Set POKETRACE_PRICING_ENABLED=true.")))
                .andExpect(content().string(containsString("Missing")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void pokemonPriceTrackerTestPageRendersForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/settings/providers/pokemon-price-tracker/test"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PokemonPriceTracker Provider Test")))
                .andExpect(content().string(containsString("Run PokemonPriceTracker Test")))
                .andExpect(content().string(containsString("Configured")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void successfulMockedPokeTraceResponseRendersMappedFieldsAndNoSecret() throws Exception {
        when(pokeTraceService.test(any())).thenReturn(new PricingProviderProbeResultView(
                "PokeTracePricingProvider",
                "https://api.poketrace.com/v1/cards/test-card",
                card("test-card", "PokeTrace", "118.45")));

        mockMvc.perform(post("/settings/providers/poketrace/test")
                        .with(csrf())
                        .param("searchOrId", "test-card"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mapped Provider Response")))
                .andExpect(content().string(containsString("Charizard ex")))
                .andExpect(content().string(containsString("Holofoil")))
                .andExpect(content().string(containsString("118.45")))
                .andExpect(content().string(containsString("PSA 10")))
                .andExpect(content().string(not(containsString("super-secret-poketrace-key"))));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void failedPokemonPriceTrackerCallShowsFriendlyErrorWithoutStackTrace() throws Exception {
        when(pokemonPriceTrackerService.test(any())).thenThrow(new PricingProviderProbeException(
                "PokemonPriceTracker test request failed. Daily credit limit exceeded."));

        mockMvc.perform(post("/settings/providers/pokemon-price-tracker/test")
                        .with(csrf())
                        .param("searchOrId", "490294"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "PokemonPriceTracker test request failed. Daily credit limit exceeded.")))
                .andExpect(content().string(not(containsString("java.lang"))))
                .andExpect(content().string(not(containsString("stack trace"))));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void comparisonPageRendersRowsAndCreatesNoSnapshots() throws Exception {
        long before = priceSnapshotRepository.count();
        when(comparisonService.compare(any(PricingProviderComparisonForm.class)))
                .thenReturn(new PricingProviderComparisonPageView(
                        "Charizard ex Obsidian Flames 101",
                        List.of(
                                new PricingProviderComparisonRowView(
                                        "PokemonApiPricingProvider",
                                        card("3852", "Pokemon API", "84.75"),
                                        new BigDecimal("114.41"),
                                        "SGD preview uses latest stored USD to SGD rate.",
                                        null),
                                new PricingProviderComparisonRowView(
                                        "PokemonPriceTrackerProvider",
                                        null,
                                        null,
                                        null,
                                        "PokemonPriceTracker subscription plan blocked this request."))));

        mockMvc.perform(post("/settings/providers/comparison/test")
                        .with(csrf())
                        .param("searchOrId", "Charizard ex Obsidian Flames 101"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pricing Provider Comparison")))
                .andExpect(content().string(containsString("PokemonApiPricingProvider")))
                .andExpect(content().string(containsString("SGD 114.41")))
                .andExpect(content().string(containsString("PokemonPriceTracker subscription plan blocked")));

        assertThat(priceSnapshotRepository.count()).isEqualTo(before);
    }

    @Test
    @WithUserDetails("owner@example.com")
    void comparisonGetPageDoesNotCallProviders() throws Exception {
        mockMvc.perform(get("/settings/providers/comparison/test"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pricing Provider Comparison")));

        verifyNoInteractions(comparisonService);
    }

    private ExternalPricingProbeCardView card(String id, String source, String price) {
        return new ExternalPricingProbeCardView(
                id,
                "Charizard ex",
                "Obsidian Flames",
                "223",
                "English",
                "Holofoil",
                "https://images.example/charizard.png",
                "TCGPLAYER",
                "USD",
                new BigDecimal(price),
                new BigDecimal("78.00"),
                new BigDecimal(price),
                new BigDecimal("130.00"),
                21,
                OffsetDateTime.parse("2026-06-08T02:15:00Z"),
                new ExternalPricingGradedPriceView("PSA 8", null, new BigDecimal("100.00"), null, 7, "USD"),
                new ExternalPricingGradedPriceView("PSA 9", null, new BigDecimal("140.00"), null, 8, "USD"),
                new ExternalPricingGradedPriceView("PSA 10", null, new BigDecimal("315.00"), null, 12, "USD"),
                "Graded data returned successfully.",
                null,
                source + " mocked exact response",
                ConfidenceRating.MEDIUM,
                "https://example.test/" + source.toLowerCase().replace(" ", "-") + "/" + id);
    }
}
