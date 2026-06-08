package com.pokemonportfolio.portfolio;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.alerts.service.PriceAlertService;
import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private PriceAlertService priceAlertService;

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
                .andExpect(content().string(containsString("SGD")))
                .andExpect(content().string(containsString("Latios")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void dashboardRendersPremiumTerminalSections() throws Exception {
        AppUser owner = owner();
        OwnedItem gainer = ownedItem(owner, "Dashboard Gainer", "25.00");
        OwnedItem loser = ownedItem(owner, "Dashboard Loser", "100.00");
        priceSnapshot(gainer, "75.00", ConfidenceRating.MEDIUM);
        priceSnapshot(loser, "60.00", ConfidenceRating.LOW);
        priceAlertService.checkAlerts(owner);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Portfolio Exchange")))
                .andExpect(content().string(containsString("Portfolio ticker")))
                .andExpect(content().string(containsString("Total Portfolio Value")))
                .andExpect(content().string(containsString("Portfolio Trend")))
                .andExpect(content().string(containsString("Top Gainers")))
                .andExpect(content().string(containsString("Top Losers")))
                .andExpect(content().string(containsString("Alerts")))
                .andExpect(content().string(containsString("Recent Activity")))
                .andExpect(content().string(containsString("Holdings Preview")))
                .andExpect(content().string(containsString("Dashboard Gainer")))
                .andExpect(content().string(containsString("Dashboard Loser")))
                .andExpect(content().string(containsString("Image not available")))
                .andExpect(content().string(containsString("SGD 75.00")))
                .andExpect(content().string(containsString("SGD -40.00")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void dashboardUsesCompactPrimaryActionsOnly() throws Exception {
        String html = mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add Card")))
                .andExpect(content().string(containsString("Refresh Prices")))
                .andExpect(content().string(containsString("Run Alert Check")))
                .andExpect(content().string(not(containsString("Run Grading Analyzer"))))
                .andExpect(content().string(not(containsString("Create Sealed Product"))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(countOccurrences(html, "<button")).isLessThanOrEqualTo(4);
    }

    @Test
    @WithUserDetails("owner@example.com")
    void dashboardHandlesUnpricedHoldingsWithoutZeroGainLoss() throws Exception {
        ownedItem(owner(), "Dashboard Unpriced", "123.45");

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dashboard Unpriced")))
                .andExpect(content().string(containsString("No price available")))
                .andExpect(content().string(not(containsString("SGD -123.45"))));
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private OwnedItem ownedItem(AppUser owner, String cardName, String purchasePrice) {
        CardForm form = new CardForm();
        form.setName(cardName);
        form.setSetName("Dashboard Redesign Set " + System.nanoTime());
        form.setCardNumber("DR-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        Card card = cardService.createManualCard(form);

        OwnedItemForm ownedItemForm = new OwnedItemForm();
        ownedItemForm.setCardId(card.getId());
        ownedItemForm.setVariant(CardVariant.STANDARD);
        ownedItemForm.setCondition(CardCondition.RAW_NEAR_MINT);
        ownedItemForm.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        ownedItemForm.setPurchaseDate(LocalDate.of(2026, 6, 8));
        ownedItemForm.setGradedStatus(GradedStatus.UNGRADED);
        return ownedItemService.addCardToPortfolio(owner, ownedItemForm);
    }

    private void priceSnapshot(OwnedItem item, String marketValueSgd, ConfidenceRating confidenceRating) {
        BigDecimal market = new BigDecimal(marketValueSgd);
        priceSnapshotRepository.save(new PriceSnapshot(
                item.getCard(),
                item.getOwnedVariant(),
                "MANUAL",
                "LOCAL",
                market,
                "SGD",
                BigDecimal.ONE.setScale(8),
                market,
                confidenceRating,
                "Dashboard redesign test snapshot",
                OffsetDateTime.now(),
                null,
                null,
                null));
    }

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
