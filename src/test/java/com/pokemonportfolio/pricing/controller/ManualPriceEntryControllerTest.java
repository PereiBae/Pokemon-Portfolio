package com.pokemonportfolio.pricing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ManualPriceEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardService cardService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Test
    @WithUserDetails("owner@example.com")
    void getManualPriceEntryPageRendersCardAndAuditFields() throws Exception {
        Card card = createCard();

        mockMvc.perform(get("/pricing/manual-entry").param("cardId", card.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Manual Price Entry")))
                .andExpect(content().string(containsString("Source Currency")))
                .andExpect(content().string(containsString("Exchange Rate To SGD")))
                .andExpect(content().string(containsString("Calculated Market Price (SGD)")))
                .andExpect(content().string(containsString("Manual Price Controller Test")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void postManualPriceEntryCreatesAppendOnlySnapshot() throws Exception {
        Card card = createCard();

        mockMvc.perform(post("/pricing/manual-entry")
                        .with(csrf())
                        .param("cardId", card.getId().toString())
                        .param("providerName", "MANUAL")
                        .param("sourcePrice", "10.00")
                        .param("sourceCurrency", "USD")
                        .param("exchangeRateUsed", "1.35000000")
                        .param("marketPriceSgd", "13.50")
                        .param("confidenceRating", "LOW")
                        .param("notes", "controller manual entry"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pricing/manual-entry?success"));

        var snapshots = priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId());

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.getFirst().getProviderName()).isEqualTo("MANUAL");
        assertThat(snapshots.getFirst().getSourceCurrency()).isEqualTo("USD");
        assertThat(snapshots.getFirst().getExchangeRateUsed()).isEqualByComparingTo("1.35000000");
        assertThat(snapshots.getFirst().getMarketPriceSgd()).isEqualByComparingTo("13.50");
    }

    @Test
    @WithUserDetails("owner@example.com")
    void postRealPriceRefreshRendersSummaryWithoutApiKey() throws Exception {
        mockMvc.perform(post("/pricing/refresh-real-prices").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Real Price Refresh")))
                .andExpect(content().string(containsString("Pokemon API provider is disabled")))
                .andExpect(content().string(containsString("Snapshots Created")))
                .andExpect(content().string(not(containsString("test-key"))))
                .andExpect(content().string(not(containsString("1997pwner"))));
    }

    private Card createCard() {
        CardForm form = new CardForm();
        form.setName("Dragonite");
        form.setSetName("Manual Price Controller Test");
        form.setCardNumber("MPC-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.PROMO);
        return cardService.createManualCard(form);
    }
}
