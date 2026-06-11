package com.pokemonportfolio.config.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.config.service.ProviderSettingsService;
import com.pokemonportfolio.pricing.provider.ExchangeRateProvider;
import com.pokemonportfolio.pricing.provider.ExchangeRateQuote;
import com.pokemonportfolio.pricing.repository.ExchangeRateSnapshotRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProviderSettingsService providerSettingsService;

    @Autowired
    private ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;

    @MockBean
    private ExchangeRateProvider exchangeRateProvider;

    @Test
    @WithUserDetails("owner@example.com")
    void providerSettingsPageShowsDefaultProviderStatus() throws Exception {
        mockMvc.perform(get("/settings/providers"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mock Pricing Provider")))
                .andExpect(content().string(containsString("Manual Price Entry")))
                .andExpect(content().string(containsString("TCGPlayer")))
                .andExpect(content().string(containsString("Test Pokemon API")))
                .andExpect(content().string(containsString("Test PokeTrace")))
                .andExpect(content().string(containsString("Test PriceTracker")))
                .andExpect(content().string(containsString("Compare Providers")))
                .andExpect(content().string(containsString("PokeTrace")))
                .andExpect(content().string(containsString("PokemonPriceTracker")))
                .andExpect(content().string(containsString("Disabled by default")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void postProviderToggleUpdatesProviderState() throws Exception {
        mockMvc.perform(post("/settings/providers")
                        .with(csrf())
                        .param("providerKey", ProviderSettingsService.MOCK_PROVIDER)
                        .param("enabled", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/providers"));

        assertThat(providerSettingsService.isEnabled(ProviderSettingsService.MOCK_PROVIDER)).isFalse();
    }

    @Test
    @WithUserDetails("owner@example.com")
    void exchangeRatePageRecordsEurToSgdRate() throws Exception {
        mockMvc.perform(get("/settings/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Exchange Rates")))
                .andExpect(content().string(containsString("EUR")))
                .andExpect(content().string(containsString("Active latest rate")));

        mockMvc.perform(post("/settings/exchange-rates")
                        .with(csrf())
                        .param("sourceCurrency", "EUR")
                        .param("targetCurrency", "SGD")
                        .param("exchangeRate", "1.46000000")
                        .param("effectiveDate", "2026-06-08")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/exchange-rates?success"));

        assertThat(exchangeRateSnapshotRepository
                .findTopBySourceCurrencyAndTargetCurrencyOrderByEffectiveAtDescIdDesc("EUR", "SGD"))
                .hasValueSatisfying(snapshot -> assertThat(snapshot.getExchangeRate())
                        .isEqualByComparingTo(new BigDecimal("1.46000000")));

        mockMvc.perform(get("/settings/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1.46000000")))
                .andExpect(content().string(containsString("Active")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void exchangeRatePageCanPreselectSourceCurrencyFromQueryParam() throws Exception {
        mockMvc.perform(get("/settings/exchange-rates").param("sourceCurrency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"EUR\" selected=\"selected\"")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void exchangeRatePageCanRefreshFrankfurterRatesWithoutApiKey() throws Exception {
        when(exchangeRateProvider.isEnabled()).thenReturn(true);
        when(exchangeRateProvider.latestRate("USD", "SGD"))
                .thenReturn(quote("USD", "1.35120000"));
        when(exchangeRateProvider.latestRate("EUR", "SGD"))
                .thenReturn(quote("EUR", "1.45670000"));

        mockMvc.perform(post("/settings/exchange-rates/refresh-defaults").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 stored")))
                .andExpect(content().string(containsString("FRANKFURTER")))
                .andExpect(content().string(not(containsString("API key"))))
                .andExpect(content().string(not(containsString("Secret"))));

        assertThat(exchangeRateSnapshotRepository
                .findTopBySourceCurrencyAndTargetCurrencyOrderByEffectiveAtDescIdDesc("USD", "SGD"))
                .hasValueSatisfying(snapshot -> {
                    assertThat(snapshot.getRateSource()).isEqualTo("FRANKFURTER");
                    assertThat(snapshot.getExchangeRate()).isEqualByComparingTo("1.35120000");
                });
        assertThat(exchangeRateSnapshotRepository
                .findTopBySourceCurrencyAndTargetCurrencyOrderByEffectiveAtDescIdDesc("EUR", "SGD"))
                .hasValueSatisfying(snapshot -> {
                    assertThat(snapshot.getRateSource()).isEqualTo("FRANKFURTER");
                    assertThat(snapshot.getExchangeRate()).isEqualByComparingTo("1.45670000");
                });
    }

    private ExchangeRateQuote quote(String sourceCurrency, String rate) {
        return new ExchangeRateQuote(
                sourceCurrency,
                "SGD",
                new BigDecimal(rate),
                "FRANKFURTER",
                OffsetDateTime.parse("2026-06-07T00:00:00Z"),
                OffsetDateTime.parse("2026-06-08T01:30:00Z"));
    }
}
