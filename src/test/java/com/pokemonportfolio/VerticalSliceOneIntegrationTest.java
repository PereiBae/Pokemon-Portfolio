package com.pokemonportfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.catalog.repository.CardRepository;
import com.pokemonportfolio.portfolio.repository.OwnedItemRepository;
import com.pokemonportfolio.portfolio.repository.PortfolioValuationSnapshotRepository;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VerticalSliceOneIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private OwnedItemRepository ownedItemRepository;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private PortfolioValuationSnapshotRepository valuationSnapshotRepository;

    @Test
    @WithUserDetails("owner@example.com")
    void loginCardPortfolioMockPriceAndValuationSnapshotFlowWorks() throws Exception {
        String uniqueNumber = "VS1-" + System.nanoTime();

        String redirect = mockMvc.perform(post("/cards")
                        .with(csrf())
                        .param("name", "Mew")
                        .param("setName", "VS1 Integration Test")
                        .param("cardNumber", uniqueNumber)
                        .param("languageMarket", "ENGLISH")
                        .param("variant", "SECRET_RARE"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        Long cardId = Long.valueOf(redirect.substring(redirect.indexOf("cardId=") + "cardId=".length()));

        mockMvc.perform(post("/portfolio/items")
                        .with(csrf())
                        .param("cardId", cardId.toString())
                        .param("condition", "RAW_NEAR_MINT")
                        .param("purchasePriceSgd", "30.00")
                        .param("purchaseDate", "2026-06-02")
                        .param("gradedStatus", "UNGRADED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SGD")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mew")));

        assertThat(cardRepository.findById(cardId)).isPresent();
        assertThat(ownedItemRepository.countByCardId(cardId)).isEqualTo(1);
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(cardId)).hasSize(1);
        assertThat(valuationSnapshotRepository.findAll()).isNotEmpty();
    }
}
