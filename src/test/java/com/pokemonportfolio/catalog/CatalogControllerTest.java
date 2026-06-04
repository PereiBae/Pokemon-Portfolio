package com.pokemonportfolio.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.provider.CardCatalogueProvider;
import com.pokemonportfolio.catalog.provider.CardCatalogueProviderException;
import com.pokemonportfolio.catalog.repository.CardRepository;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.OfficialCardSearchPage;
import com.pokemonportfolio.catalog.service.OfficialCardSearchRequest;
import com.pokemonportfolio.catalog.service.OfficialCardSearchResult;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.VerificationStatus;
import com.pokemonportfolio.portfolio.repository.OwnedItemRepository;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.trade.service.TradeCreateForm;
import com.pokemonportfolio.trade.service.TradeTransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private OwnedItemRepository ownedItemRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private TradeTransactionService tradeTransactionService;

    @Test
    @WithUserDetails("owner@example.com")
    void searchPageRendersOfficialResultsForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/catalog/search")
                        .param("q", "Charizard")
                        .param("cardName", "Charizard")
                        .param("setName", "Obsidian Flames")
                        .param("cardNumber", "223")
                        .param("rarity", "Special Illustration Rare")
                        .param("page", "1")
                        .param("pageSize", "25"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Search Official Catalogue")))
                .andExpect(content().string(containsString("catalog-search-form")))
                .andExpect(content().string(containsString("card-cell-content")))
                .andExpect(content().string(containsString("Charizard ex")))
                .andExpect(content().string(containsString("Obsidian Flames")))
                .andExpect(content().string(containsString("https://images.example/charizard-small.png")))
                .andExpect(content().string(containsString("Standard, Holo, Reverse Holo")))
                .andExpect(content().string(containsString("Page 1 of 2")))
                .andExpect(content().string(containsString("Next")))
                .andExpect(content().string(containsString("Pokemon TCG API")))
                .andExpect(content().string(containsString("Verified")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void addToPortfolioImportCarriesSelectedVariantToPortfolioForm() throws Exception {
        mockMvc.perform(post("/catalog/import")
                        .with(csrf())
                        .param("source", "POKEMON_TCG_API")
                        .param("externalCardId", "sv3-223")
                        .param("action", "portfolio")
                        .param("variant", "HOLO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/portfolio/add?cardId=*&variant=HOLO"));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void officialCatalogueCanReturnImportedCardToIncomingTradeSelection() throws Exception {
        var owner = appUserRepository.findByUsername("owner@example.com").orElseThrow();
        var trade = tradeTransactionService.createDraft(owner, tradeForm());

        mockMvc.perform(get("/catalog/search")
                        .param("q", "Charizard")
                        .param("returnTradeId", trade.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Select for Trade")))
                .andExpect(content().string(containsString("Trade #")))
                .andExpect(content().string(containsString("returnTradeId")));

        String redirect = mockMvc.perform(post("/catalog/import")
                        .with(csrf())
                        .param("source", "POKEMON_TCG_API")
                        .param("externalCardId", "sv3-223")
                        .param("action", "tradeIncoming")
                        .param("returnTradeId", trade.getId().toString())
                        .param("variant", "HOLO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/trades/*?incomingCardId=*&variant=HOLO"))
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        Long cardId = Long.valueOf(redirect.substring(
                redirect.indexOf("incomingCardId=") + "incomingCardId=".length(),
                redirect.indexOf("&variant")));
        assertThat(cardRepository.findById(cardId).orElseThrow().getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(ownedItemRepository.countByCardId(cardId)).isZero();

        mockMvc.perform(get(redirect))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Selected Incoming Card")))
                .andExpect(content().string(containsString("Charizard ex")))
                .andExpect(content().string(containsString("https://images.example/charizard-large.png")))
                .andExpect(content().string(containsString("Holo")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void searchPageRendersPlaceholderWhenImageIsMissing() throws Exception {
        mockMvc.perform(get("/catalog/search").param("q", "noimage"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Image not available")))
                .andExpect(content().string(containsString("Mew ex")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void apiUnavailableReturnsFriendlyError() throws Exception {
        mockMvc.perform(get("/catalog/search").param("q", "fail"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Official catalogue is unavailable right now")))
                .andExpect(content().string(containsString("Create a custom unverified card")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void manualCardPageWarnsThatCustomCardsAreUnverified() throws Exception {
        mockMvc.perform(get("/cards/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create Custom / Unverified Card")))
                .andExpect(content().string(containsString(
                        "Manual cards are unverified and may not have reliable pricing or analytics.")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void portfolioCanAddVerifiedImportedCard() throws Exception {
        String redirect = mockMvc.perform(post("/catalog/import")
                        .with(csrf())
                        .param("source", "POKEMON_TCG_API")
                        .param("externalCardId", "sv3-223")
                        .param("action", "portfolio"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        Long cardId = Long.valueOf(redirect.substring(redirect.indexOf("cardId=") + "cardId=".length()));

        Card card = cardRepository.findById(cardId).orElseThrow();
        assertThat(card.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(card.getCatalogSource()).isEqualTo(CatalogSource.POKEMON_TCG_API);

        mockMvc.perform(post("/portfolio/items")
                        .with(csrf())
                        .param("cardId", cardId.toString())
                        .param("variant", "REVERSE_HOLO")
                        .param("condition", "RAW_NEAR_MINT")
                        .param("purchasePriceSgd", "120.00")
                        .param("purchaseDate", "2026-06-03")
                        .param("gradedStatus", "UNGRADED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        assertThat(ownedItemRepository.countByCardId(cardId)).isEqualTo(1);
        assertThat(ownedItemRepository.findAll().getFirst().getOwnedVariant()).isEqualTo(CardVariant.REVERSE_HOLO);

        mockMvc.perform(get("/portfolio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("https://images.example/charizard-small.png")))
                .andExpect(content().string(containsString("Verified")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void portfolioPageRendersPlaceholderForManualCardWithoutImage() throws Exception {
        var owner = appUserRepository.findByUsername("owner@example.com").orElseThrow();
        CardForm cardForm = new CardForm();
        cardForm.setName("Manual Portfolio Card");
        cardForm.setSetName("Manual Portfolio Set " + System.nanoTime());
        cardForm.setCardNumber("MP-" + System.nanoTime());
        cardForm.setLanguageMarket(LanguageMarket.ENGLISH);
        cardForm.setVariant(CardVariant.PROMO);
        Card card = cardService.createManualCard(cardForm);

        OwnedItemForm itemForm = new OwnedItemForm();
        itemForm.setCardId(card.getId());
        itemForm.setCondition(CardCondition.RAW_NEAR_MINT);
        itemForm.setPurchasePriceSgd(new BigDecimal("12.00"));
        itemForm.setPurchaseDate(LocalDate.of(2026, 6, 3));
        itemForm.setGradedStatus(GradedStatus.UNGRADED);
        ownedItemService.addCardToPortfolio(owner, itemForm);

        mockMvc.perform(get("/portfolio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Manual Portfolio Card")))
                .andExpect(content().string(containsString("Image not available")))
                .andExpect(content().string(containsString("Unverified")));
    }

    @TestConfiguration
    static class FakeProviderConfig {

        @Bean
        CardCatalogueProvider fakeOfficialCardProvider() {
            return new CardCatalogueProvider() {
                @Override
                public CatalogSource source() {
                    return CatalogSource.POKEMON_TCG_API;
                }

                @Override
                public boolean isEnabled() {
                    return true;
                }

                @Override
                public OfficialCardSearchPage search(OfficialCardSearchRequest request) {
                    String query = request.getKeyword();
                    if ("fail".equalsIgnoreCase(query)) {
                        throw new CardCatalogueProviderException("Provider unavailable");
                    }
                    if ("noimage".equalsIgnoreCase(query)) {
                        return new OfficialCardSearchPage(List.of(mewWithoutImage()), request.getPage(), request.getPageSize(), 1);
                    }
                    return new OfficialCardSearchPage(List.of(charizard()), request.getPage(), request.getPageSize(), 30);
                }

                @Override
                public OfficialCardSearchResult findByExternalId(String externalCardId) {
                    return charizard();
                }

                private OfficialCardSearchResult charizard() {
                    return new OfficialCardSearchResult(
                            "sv3-223",
                            "Charizard ex",
                            "sv3",
                            "Obsidian Flames",
                            "Scarlet & Violet",
                            LocalDate.of(2023, 8, 11),
                            "223",
                            "Special Illustration Rare",
                            "https://images.example/charizard-small.png",
                            "https://images.example/charizard-large.png",
                            "https://prices.example/charizard",
                            LanguageMarket.ENGLISH,
                            CatalogSource.POKEMON_TCG_API,
                            List.of(CardVariant.STANDARD, CardVariant.HOLO, CardVariant.REVERSE_HOLO));
                }

                private OfficialCardSearchResult mewWithoutImage() {
                    return new OfficialCardSearchResult(
                            "sv4pt5-232",
                            "Mew ex",
                            "sv4pt5",
                            "Paldean Fates",
                            "Scarlet & Violet",
                            LocalDate.of(2024, 1, 26),
                            "232",
                            "Special Illustration Rare",
                            null,
                            null,
                            null,
                            LanguageMarket.ENGLISH,
                            CatalogSource.POKEMON_TCG_API,
                            List.of());
                }
            };
        }
    }

    private TradeCreateForm tradeForm() {
        TradeCreateForm form = new TradeCreateForm();
        form.setName("Catalogue Trade " + System.nanoTime());
        form.setOutgoingTradePercentage(new BigDecimal("100.00"));
        form.setIncomingTradePercentage(new BigDecimal("100.00"));
        return form;
    }
}
