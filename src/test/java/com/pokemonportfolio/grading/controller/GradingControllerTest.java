package com.pokemonportfolio.grading.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.pokemonportfolio.grading.service.GradingFeeService;
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
class GradingControllerTest {

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
    private GradingFeeService gradingFeeService;

    @Test
    @WithUserDetails("owner@example.com")
    void gradingAnalyzerPageRendersForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/grading"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PSA Grading Analyzer")))
                .andExpect(content().string(containsString("Run Analysis")))
                .andExpect(content().string(containsString("Only active ungraded card holdings are eligible")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void gradingAnalyzerPagePrefillsLatestRawValueAndRendersVerifiedCardImage() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedCard(owner, verifiedCardWithImage(), "100.00");
        snapshot(item.getCard(), "140.00", ConfidenceRating.MEDIUM);

        mockMvc.perform(get("/grading").param("ownedItemId", item.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Controller Verified Grading Card")))
                .andExpect(content().string(containsString("https://images.example/grading-small.png")))
                .andExpect(content().string(containsString("value=\"140.00\"")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void postingAnalysisRedirectsToSavedResultAndRendersScenarios() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedCard(owner, manualCard("Controller Grading Result Card"), "100.00");
        Long feeId = gradingFeeService.defaultPsaFee().getId();

        String resultUrl = mockMvc.perform(post("/grading/analyze")
                        .with(csrf())
                        .param("ownedItemId", item.getId().toString())
                        .param("rawValueSgd", "150.00")
                        .param("psa8ValueSgd", "120.00")
                        .param("psa9ValueSgd", "140.00")
                        .param("psa10ValueSgd", "145.00")
                        .param("gradingFeeId", feeId.toString())
                        .param("estimatedTurnaroundDays", "65")
                        .param("opportunityCostSgd", "0.00")
                        .param("notes", "Controller scenario test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/grading/*"))
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        mockMvc.perform(get(resultUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Grading Analysis Result")))
                .andExpect(content().string(containsString("Controller Grading Result Card")))
                .andExpect(content().string(containsString("Conservative")))
                .andExpect(content().string(containsString("Balanced")))
                .andExpect(content().string(containsString("Aggressive")))
                .andExpect(content().string(containsString("Raw value is higher than PSA 8 value")))
                .andExpect(content().string(containsString("Raw value is higher than PSA 9 value")))
                .andExpect(content().string(containsString("Do Not Grade")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void dashboardLinksToGradingAnalyzer() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Run Grading Analyzer")))
                .andExpect(content().string(containsString("PSA Grading Analyzer")));
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private OwnedItem ownedCard(AppUser owner, Card card, String purchasePrice) {
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(card.getId());
        form.setVariant(CardVariant.STANDARD);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        form.setPurchaseDate(LocalDate.of(2026, 6, 4));
        form.setGradedStatus(GradedStatus.UNGRADED);
        return ownedItemService.addCardToPortfolio(owner, form);
    }

    private Card manualCard(String name) {
        CardForm form = new CardForm();
        form.setName(name);
        form.setSetName("Grading Controller Set " + System.nanoTime());
        form.setCardNumber("GC-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
    }

    private Card verifiedCardWithImage() {
        return cardService.importOfficialCard(new OfficialCardSearchResult(
                "grading-" + System.nanoTime(),
                "Controller Verified Grading Card",
                "grading-set",
                "Grading Controller Set",
                "Test Series",
                LocalDate.of(2026, 1, 1),
                "201",
                "Illustration Rare",
                "https://images.example/grading-small.png",
                "https://images.example/grading-large.png",
                "https://prices.example/grading-card",
                LanguageMarket.ENGLISH,
                CatalogSource.POKEMON_TCG_API,
                List.of(CardVariant.STANDARD, CardVariant.HOLO)));
    }

    private PriceSnapshot snapshot(Card card, String marketPrice, ConfidenceRating confidenceRating) {
        return priceSnapshotRepository.save(new PriceSnapshot(
                card,
                "MANUAL",
                new BigDecimal(marketPrice),
                "SGD",
                BigDecimal.ONE,
                new BigDecimal(marketPrice),
                confidenceRating,
                "Grading controller test snapshot.",
                OffsetDateTime.now()));
    }
}
