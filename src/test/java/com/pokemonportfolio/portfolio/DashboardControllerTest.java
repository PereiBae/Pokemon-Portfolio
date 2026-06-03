package com.pokemonportfolio.portfolio;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithUserDetails("owner@example.com")
    void dashboardRendersPortfolioValueInSgdAfterAddingCard() throws Exception {
        String uniqueNumber = String.valueOf(System.nanoTime());

        String redirect = mockMvc.perform(post("/cards")
                        .with(csrf())
                        .param("name", "Latios")
                        .param("setName", "VS1 Dashboard Test")
                        .param("cardNumber", uniqueNumber)
                        .param("languageMarket", "ENGLISH")
                        .param("variant", "PROMO"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        String cardId = redirect.substring(redirect.indexOf("cardId=") + "cardId=".length());

        mockMvc.perform(post("/portfolio/items")
                        .with(csrf())
                        .param("cardId", cardId)
                        .param("condition", "RAW_NEAR_MINT")
                        .param("purchasePriceSgd", "25.00")
                        .param("purchaseDate", "2026-06-02")
                        .param("gradedStatus", "UNGRADED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SGD")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Latios")));
    }
}
