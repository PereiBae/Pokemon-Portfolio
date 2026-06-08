package com.pokemonportfolio.alerts.controller;

import static org.hamcrest.Matchers.containsString;
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
import com.pokemonportfolio.catalog.service.OfficialCardSearchResult;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
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
import java.util.List;
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
class AlertControllerTest {

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
    void alertsPageRendersForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Price Alerts")))
                .andExpect(content().string(containsString("Check Alerts")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void manualCheckCreatesAlertForManualCardWithoutImage() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, manualCard("Manual Alert Card"), "80.00");
        snapshot(item.getCard(), "91.00");

        mockMvc.perform(post("/alerts/check").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/alerts"));

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Manual Alert Card")))
                .andExpect(content().string(containsString("Image not available")))
                .andExpect(content().string(containsString("SGD 11.00")))
                .andExpect(content().string(containsString("Active")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void alertsPageRendersVerifiedCardImage() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, verifiedCardWithImage(), "80.00");
        snapshot(item.getCard(), "91.00");
        priceAlertService.checkAlerts(owner);

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Verified Alert Card")))
                .andExpect(content().string(containsString("https://images.example/verified-alert-small.png")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void dismissingAlertRetainsItInHistory() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, manualCard("Dismiss Alert Card"), "80.00");
        snapshot(item.getCard(), "91.00");
        Long alertId = priceAlertService.checkAlerts(owner).getFirst().getId();

        mockMvc.perform(post("/alerts/{alertId}/dismiss", alertId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/alerts"));

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dismiss Alert Card")))
                .andExpect(content().string(containsString("Dismissed")))
                .andExpect(content().string(containsString("Dismissed records are retained")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void dashboardDisplaysActiveAlertCount() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, manualCard("Dashboard Alert Card"), "80.00");
        snapshot(item.getCard(), "91.00");
        priceAlertService.checkAlerts(owner);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 active signal(s)")))
                .andExpect(content().string(containsString("Dashboard Alert Card")))
                .andExpect(content().string(containsString("href=\"/alerts\"")));
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private OwnedItem ownedItem(AppUser owner, Card card, String purchasePrice) {
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(card.getId());
        form.setVariant(CardVariant.STANDARD);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        form.setPurchaseDate(LocalDate.of(2026, 6, 3));
        form.setGradedStatus(GradedStatus.UNGRADED);
        return ownedItemService.addCardToPortfolio(owner, form);
    }

    private Card manualCard(String name) {
        CardForm form = new CardForm();
        form.setName(name);
        form.setSetName("Alert Controller Set " + System.nanoTime());
        form.setCardNumber("AC-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
    }

    private Card verifiedCardWithImage() {
        return cardService.importOfficialCard(new OfficialCardSearchResult(
                "alert-" + System.nanoTime(),
                "Verified Alert Card",
                "alert-set",
                "Alert Test Set",
                "Test Series",
                LocalDate.of(2026, 1, 1),
                "101",
                "Rare",
                "https://images.example/verified-alert-small.png",
                "https://images.example/verified-alert-large.png",
                "https://prices.example/verified-alert",
                LanguageMarket.ENGLISH,
                CatalogSource.POKEMON_TCG_API,
                List.of(CardVariant.STANDARD, CardVariant.HOLO)));
    }

    private PriceSnapshot snapshot(Card card, String marketPrice) {
        return priceSnapshotRepository.save(new PriceSnapshot(
                card,
                "MANUAL",
                new BigDecimal(marketPrice),
                "SGD",
                BigDecimal.ONE,
                new BigDecimal(marketPrice),
                ConfidenceRating.MEDIUM,
                "Controller test price snapshot.",
                OffsetDateTime.now()));
    }
}
