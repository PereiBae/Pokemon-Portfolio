package com.pokemonportfolio.pricing.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingCardView;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPsaPriceView;
import com.pokemonportfolio.pricing.service.PokemonApiProviderStatusView;
import com.pokemonportfolio.pricing.service.PokemonApiProviderTestException;
import com.pokemonportfolio.pricing.service.PokemonApiProviderTestLookupType;
import com.pokemonportfolio.pricing.service.PokemonApiProviderTestResultView;
import com.pokemonportfolio.pricing.service.PokemonApiProviderTestService;
import java.math.BigDecimal;
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
class PokemonApiProviderTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PokemonApiProviderTestService testService;

    @BeforeEach
    void setUp() {
        when(testService.status()).thenReturn(new PokemonApiProviderStatusView(
                "PokemonApiPricingProvider",
                true,
                true,
                "https://pokemon-tcg-api.p.rapidapi.com"));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void getTestPageRendersForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/settings/providers/pokemon-api/test"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pokemon API Provider Test")))
                .andExpect(content().string(containsString("Run Provider Test")))
                .andExpect(content().string(containsString("RapidAPI Key")))
                .andExpect(content().string(containsString("Configured")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void disabledProviderShowsFriendlyMessage() throws Exception {
        when(testService.status()).thenReturn(new PokemonApiProviderStatusView(
                "PokemonApiPricingProvider",
                false,
                true,
                "https://pokemon-tcg-api.p.rapidapi.com"));
        when(testService.test(any())).thenThrow(new PokemonApiProviderTestException(
                "Pokemon API provider is disabled. Set POKEMON_API_PRICING_ENABLED=true."));

        mockMvc.perform(post("/settings/providers/pokemon-api/test")
                        .with(csrf())
                        .param("lookupType", "CARD")
                        .param("searchOrId", "3852"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Pokemon API provider is disabled. Set POKEMON_API_PRICING_ENABLED=true.")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void missingApiKeyShowsFriendlyMessage() throws Exception {
        when(testService.status()).thenReturn(new PokemonApiProviderStatusView(
                "PokemonApiPricingProvider",
                true,
                false,
                "https://pokemon-tcg-api.p.rapidapi.com"));
        when(testService.test(any())).thenThrow(new PokemonApiProviderTestException(
                "RapidAPI key is missing. Set POKEMON_API_RAPIDAPI_KEY."));

        mockMvc.perform(post("/settings/providers/pokemon-api/test")
                        .with(csrf())
                        .param("lookupType", "CARD")
                        .param("searchOrId", "3852"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "RapidAPI key is missing. Set POKEMON_API_RAPIDAPI_KEY.")))
                .andExpect(content().string(containsString("Missing")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void successfulMockedProviderResponseRendersMappedFields() throws Exception {
        when(testService.test(any())).thenReturn(new PokemonApiProviderTestResultView(
                PokemonApiProviderTestLookupType.CARD,
                "PokemonApiPricingProvider",
                "https://pokemon-tcg-api.p.rapidapi.com/cards/3852",
                cardView(),
                null));

        mockMvc.perform(post("/settings/providers/pokemon-api/test")
                        .with(csrf())
                        .param("lookupType", "CARD")
                        .param("searchOrId", "3852"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mapped Provider Response")))
                .andExpect(content().string(containsString("PokemonApiPricingProvider")))
                .andExpect(content().string(containsString("Giratina VSTAR")))
                .andExpect(content().string(containsString("Crown Zenith")))
                .andExpect(content().string(containsString("https://images.example/giratina.png")))
                .andExpect(content().string(containsString("146.69")))
                .andExpect(content().string(containsString("163.71")))
                .andExpect(content().string(containsString("PSA 10")))
                .andExpect(content().string(containsString("2941.00")))
                .andExpect(content().string(containsString("5")))
                .andExpect(content().string(containsString("https://pokemon-tcg-api.p.rapidapi.com/cards/3852")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void failedProviderCallShowsFriendlyErrorWithoutStackTrace() throws Exception {
        when(testService.test(any())).thenThrow(new PokemonApiProviderTestException(
                "Pokemon API test request failed. Upstream unavailable."));

        mockMvc.perform(post("/settings/providers/pokemon-api/test")
                        .with(csrf())
                        .param("lookupType", "CARD")
                        .param("searchOrId", "3852"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Pokemon API test request failed. Upstream unavailable.")))
                .andExpect(content().string(not(containsString("java.lang"))))
                .andExpect(content().string(not(containsString("stack trace"))));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void apiKeyIsNotDisplayedInUi() throws Exception {
        mockMvc.perform(get("/settings/providers/pokemon-api/test"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Configured")))
                .andExpect(content().string(not(containsString("super-secret-rapidapi-key"))));
    }

    private PokemonApiPricingCardView cardView() {
        return new PokemonApiPricingCardView(
                3852L,
                "Giratina VSTAR",
                "Crown Zenith",
                "GG69",
                "https://images.example/giratina.png",
                "USD",
                new BigDecimal("146.69"),
                new BigDecimal("163.71"),
                new PokemonApiPsaPriceView(8, new BigDecimal("600.00"), 3, "USD"),
                new PokemonApiPsaPriceView(9, new BigDecimal("1200.00"), 4, "USD"),
                new PokemonApiPsaPriceView(10, new BigDecimal("2941.00"), 5, "USD"));
    }
}
