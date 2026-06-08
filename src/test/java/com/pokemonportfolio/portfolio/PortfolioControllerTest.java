package com.pokemonportfolio.portfolio;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardService cardService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Test
    @WithUserDetails("owner@example.com")
    void addPageRendersCardOptionWithSetName() throws Exception {
        CardForm form = new CardForm();
        form.setName("Umbreon");
        form.setSetName("VS1 Add Page Test");
        form.setCardNumber("ADD-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.ALTERNATE_ART);

        Card card = cardService.createManualCard(form);

        mockMvc.perform(get("/portfolio/add").param("cardId", card.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Umbreon")))
                .andExpect(content().string(containsString("VS1 Add Page Test")))
                .andExpect(content().string(containsString("Owned Variant")))
                .andExpect(content().string(containsString("Alternate Art")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void sellFlowRemovesItemFromActivePortfolioAndRendersDisposalHistory() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, "Controller Sell Card", "100.00");

        mockMvc.perform(post("/portfolio/items/{id}/sell", item.getId())
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("salePriceSgd", "120.00")
                        .param("feesSgd", "5.00")
                        .param("disposalDate", "2026-06-04")
                        .param("notes", "Sold to local collector"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portfolio/disposals"));

        mockMvc.perform(get("/portfolio"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Controller Sell Card"))))
                .andExpect(content().string(containsString("Delete Mistake is only for accidental duplicate")));

        mockMvc.perform(get("/portfolio/disposals"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disposal History")))
                .andExpect(content().string(containsString("Controller Sell Card")))
                .andExpect(content().string(containsString("Sold")))
                .andExpect(content().string(containsString("SGD 15.00")))
                .andExpect(content().string(containsString("Sold to local collector")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void dashboardRendersRealizedGainLoss() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, "Controller Dashboard Realised Card", "100.00");

        mockMvc.perform(post("/portfolio/items/{id}/sell", item.getId())
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("salePriceSgd", "125.00")
                        .param("feesSgd", "0.00")
                        .param("disposalDate", "2026-06-04"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Realised Gain/Loss")))
                .andExpect(content().string(containsString("Total Performance")))
                .andExpect(content().string(containsString("SGD 25.00")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void portfolioRendersGenericFallbackLowConfidenceBadge() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, "Controller Generic Fallback Card", "100.00");
        priceSnapshotRepository.save(new PriceSnapshot(
                item.getCard(),
                item.getOwnedVariant(),
                "POKEMON_API",
                "TCGPLAYER",
                new BigDecimal("120.00"),
                "USD",
                new BigDecimal("1.35000000"),
                new BigDecimal("162.00"),
                ConfidenceRating.LOW,
                "Generic raw price used; provider did not supply variant-specific pricing.",
                OffsetDateTime.now(),
                "https://example.test/pokemon-api/cards/1",
                null,
                "source_field=tcg_player.market_price;match=GENERIC_RAW_FALLBACK;variant=STANDARD;single_provider=true"));

        mockMvc.perform(get("/portfolio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Controller Generic Fallback Card")))
                .andExpect(content().string(containsString("LOW")))
                .andExpect(content().string(containsString("POKEMON_API / TCGPLAYER / USD")))
                .andExpect(content().string(containsString("Generic raw fallback")))
                .andExpect(content().string(containsString("Not variant-specific")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void portfolioAndDashboardRenderMissingLatestPriceAsUnavailable() throws Exception {
        AppUser owner = owner();
        ownedItem(owner, "Controller Unpriced Card", "123.45");

        mockMvc.perform(get("/portfolio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Controller Unpriced Card")))
                .andExpect(content().string(containsString("No price available")))
                .andExpect(content().string(not(containsString("SGD -123.45"))));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Controller Unpriced Card")))
                .andExpect(content().string(containsString("No price available")))
                .andExpect(content().string(not(containsString("SGD -123.45"))));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void disposalHistoryPageRendersForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/portfolio/disposals"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disposal History")))
                .andExpect(content().string(containsString("Active Portfolio")));
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private OwnedItem ownedItem(AppUser owner, String cardName, String purchasePrice) {
        Card card = createCard(cardName, "Controller Disposal Set " + System.nanoTime(), "PC-" + System.nanoTime());
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(card.getId());
        form.setVariant(CardVariant.STANDARD);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        form.setPurchaseDate(LocalDate.of(2026, 6, 4));
        form.setGradedStatus(GradedStatus.UNGRADED);
        return ownedItemService.addCardToPortfolio(owner, form);
    }

    private Card createCard(String name, String setName, String number) {
        CardForm form = new CardForm();
        form.setName(name);
        form.setSetName(setName);
        form.setCardNumber(number);
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.ALTERNATE_ART);
        return cardService.createManualCard(form);
    }
}
