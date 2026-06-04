package com.pokemonportfolio.trade.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.repository.OwnedItemDisposalRepository;
import com.pokemonportfolio.portfolio.repository.OwnedItemRepository;
import com.pokemonportfolio.portfolio.service.OwnedItemDisposalForm;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioDisposalService;
import com.pokemonportfolio.trade.service.TradeCreateForm;
import com.pokemonportfolio.trade.service.TradeIncomingItemForm;
import com.pokemonportfolio.trade.service.TradeOutgoingItemForm;
import com.pokemonportfolio.trade.service.TradeTransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private PortfolioDisposalService disposalService;

    @Autowired
    private TradeTransactionService tradeTransactionService;

    @Autowired
    private OwnedItemDisposalRepository disposalRepository;

    @Autowired
    private OwnedItemRepository ownedItemRepository;

    @Test
    @WithUserDetails("owner@example.com")
    void tradePagesRenderForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/trades"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trade Analyzer")))
                .andExpect(content().string(containsString("Create Trade")));

        mockMvc.perform(get("/trades/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create Trade")))
                .andExpect(content().string(containsString("analysis-only")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void createTradePostRedirectsToDraftDetail() throws Exception {
        mockMvc.perform(post("/trades")
                        .with(csrf())
                        .param("name", "Controller Draft Trade")
                        .param("outgoingTradePercentage", "100.00")
                        .param("incomingTradePercentage", "100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/trades/*"));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void analysisOnlyControllerActionDoesNotChangePortfolio() throws Exception {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Controller Analysis Outgoing", "100.00");
        Card incoming = card("Controller Analysis Incoming");
        Long tradeId = draftTradeWithItems(owner, outgoing, incoming, "100.00", "105.00");

        mockMvc.perform(post("/trades/{tradeId}/analyze", tradeId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trades/" + tradeId));

        assertThatActive(owner, outgoing.getId());
        mockMvc.perform(get("/trades/{tradeId}", tradeId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Review & Execute")))
                .andExpect(content().string(containsString("Search official catalogue")))
                .andExpect(content().string(containsString("Advanced Official Search")))
                .andExpect(content().string(containsString("Override Value SGD")))
                .andExpect(content().string(containsString("Balanced")))
                .andExpect(content().string(containsString("SGD 105.00")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void nullIncomingCardSelectionShowsValidationMessageWithoutServerError() throws Exception {
        AppUser owner = owner();
        var trade = tradeTransactionService.createDraft(owner, tradeForm());

        mockMvc.perform(post("/trades/{tradeId}/incoming", trade.getId())
                        .with(csrf())
                        .param("overrideValueSgd", "100.00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Please select an incoming card or sealed product.")))
                .andExpect(content().string(containsString("No incoming items yet.")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void incomingSideRendersSearchFlowWithoutGenericCatalogueDropdown() throws Exception {
        AppUser owner = owner();
        var trade = tradeTransactionService.createDraft(owner, tradeForm());

        mockMvc.perform(get("/trades/{tradeId}", trade.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("incoming-search-form")))
                .andExpect(content().string(containsString("Search official catalogue")))
                .andExpect(content().string(containsString("Create Custom Incoming Card")))
                .andExpect(content().string(containsString("Create Custom Incoming Sealed Product")))
                .andExpect(content().string(containsString("Incoming sealed product")))
                .andExpect(content().string(containsString("Please search and select an incoming card, or choose a sealed product below.")))
                .andExpect(content().string(not(containsString("Catalogue card"))))
                .andExpect(content().string(not(containsString("Select card"))));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void selectedIncomingCardSummaryRendersFromOfficialCatalogueSelection() throws Exception {
        AppUser owner = owner();
        var trade = tradeTransactionService.createDraft(owner, tradeForm());
        Card incoming = verifiedCard("Selected Incoming Summary");

        mockMvc.perform(get("/trades/{tradeId}", trade.getId())
                        .param("incomingCardId", incoming.getId().toString())
                        .param("variant", "HOLO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Selected Incoming Card")))
                .andExpect(content().string(containsString("Selected Incoming Summary")))
                .andExpect(content().string(containsString("https://images.example/selected-incoming-summary-large.png")))
                .andExpect(content().string(containsString("Verified")))
                .andExpect(content().string(containsString("Pokemon TCG API")))
                .andExpect(content().string(containsString("Holo")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void addIncomingWithSelectedCardAndOverrideCreatesTradeItemOnly() throws Exception {
        AppUser owner = owner();
        var trade = tradeTransactionService.createDraft(owner, tradeForm());
        Card incoming = verifiedCard("Override Incoming");
        int activeCountBefore = ownedItemService.listActiveItems(owner).size();

        mockMvc.perform(post("/trades/{tradeId}/incoming", trade.getId())
                        .with(csrf())
                        .param("cardId", incoming.getId().toString())
                        .param("variant", "HOLO")
                        .param("condition", "RAW_NEAR_MINT")
                        .param("overrideValueSgd", "125.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trades/" + trade.getId()));

        var detail = tradeTransactionService.detailView(owner, trade.getId());
        org.assertj.core.api.Assertions.assertThat(detail.incomingItems()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(detail.incomingItems().getFirst().overrideValueSgd())
                .isEqualByComparingTo("125.00");
        org.assertj.core.api.Assertions.assertThat(ownedItemRepository.countByCardId(incoming.getId())).isZero();
        org.assertj.core.api.Assertions.assertThat(ownedItemService.listActiveItems(owner)).hasSize(activeCountBefore);
    }

    @Test
    @WithUserDetails("owner@example.com")
    void addIncomingWithSelectedCardMissingMarketAndBlankOverrideShowsFriendlyValidation() throws Exception {
        AppUser owner = owner();
        var trade = tradeTransactionService.createDraft(owner, tradeForm());
        Card incoming = verifiedCard("Missing Override Incoming");

        mockMvc.perform(post("/trades/{tradeId}/incoming", trade.getId())
                        .with(csrf())
                        .param("cardId", incoming.getId().toString())
                        .param("variant", "HOLO")
                        .param("condition", "RAW_NEAR_MINT"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "No market value found for this incoming card. Enter an Override Value SGD to continue.")))
                .andExpect(content().string(containsString("Selected Incoming Card")))
                .andExpect(content().string(containsString("Missing Override Incoming")))
                .andExpect(content().string(containsString("Holo")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void customIncomingCardFallbackReturnsToPendingTradeSelection() throws Exception {
        AppUser owner = owner();
        var trade = tradeTransactionService.createDraft(owner, tradeForm());

        String redirect = mockMvc.perform(post("/cards")
                        .with(csrf())
                        .param("returnTradeId", trade.getId().toString())
                        .param("name", "Custom Incoming Fallback")
                        .param("setName", "Custom Incoming Set")
                        .param("cardNumber", "CIF-1")
                        .param("languageMarket", "ENGLISH")
                        .param("variant", "PROMO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/trades/*?incomingCardId=*&variant=PROMO"))
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        Long cardId = Long.valueOf(redirect.substring(
                redirect.indexOf("incomingCardId=") + "incomingCardId=".length(),
                redirect.indexOf("&variant")));
        org.assertj.core.api.Assertions.assertThat(ownedItemRepository.countByCardId(cardId)).isZero();

        mockMvc.perform(get(redirect))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Selected Incoming Card")))
                .andExpect(content().string(containsString("Custom Incoming Fallback")))
                .andExpect(content().string(containsString("Unverified")))
                .andExpect(content().string(containsString("Promo")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void outgoingSideOnlyListsActiveOwnedItems() throws Exception {
        AppUser owner = owner();
        OwnedItem active = ownedItem(owner, "Active Trade Option", "50.00");
        OwnedItem sold = ownedItem(owner, "Sold Trade Option", "50.00");
        OwnedItem traded = ownedItem(owner, "Traded Trade Option", "50.00");
        OwnedItem deleted = ownedItem(owner, "Deleted Trade Option", "50.00");
        disposalService.sellItem(owner, sold.getId(), saleForm("60.00"));
        disposalService.tradeAwayItem(owner, traded.getId(), tradeAwayForm("65.00"));
        disposalService.deleteMistake(owner, deleted.getId(), deleteForm());
        var trade = tradeTransactionService.createDraft(owner, tradeForm());

        mockMvc.perform(get("/trades/{tradeId}", trade.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(active.getCard().getName())))
                .andExpect(content().string(not(containsString(sold.getCard().getName()))))
                .andExpect(content().string(not(containsString(traded.getCard().getName()))))
                .andExpect(content().string(not(containsString(deleted.getCard().getName()))));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void executeControllerActionCreatesTradeAccountingAndDisposalHistoryLink() throws Exception {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Controller Execute Outgoing", "200.00");
        Card incoming = card("Controller Execute Incoming");
        Long tradeId = draftTradeWithItems(owner, outgoing, incoming, "500.00", "500.00");

        mockMvc.perform(post("/trades/{tradeId}/execute", tradeId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trades/" + tradeId));

        assertThatTraded(owner, outgoing.getId());
        var disposal = disposalRepository.findByOwnedItemId(outgoing.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(disposal.getTradeTransactionId()).isEqualTo(tradeId);
        org.assertj.core.api.Assertions.assertThat(disposal.getRealizedGainLossSgd()).isEqualByComparingTo("300.00");
        org.assertj.core.api.Assertions.assertThat(tradeTransactionService.detailView(owner, tradeId).incomingItems().getFirst()
                        .incomingOwnedItemId())
                .isNotNull();

        mockMvc.perform(get("/portfolio/disposals"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trade #" + tradeId)));
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private Long draftTradeWithItems(
            AppUser owner,
            OwnedItem outgoing,
            Card incoming,
            String outgoingAgreedValue,
            String incomingAgreedValue) {
        var trade = tradeTransactionService.createDraft(owner, tradeForm());
        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), outgoingAgreedValue));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incoming.getId(), incomingAgreedValue));
        return trade.getId();
    }

    private void assertThatActive(AppUser owner, Long ownedItemId) {
        org.assertj.core.api.Assertions.assertThat(ownedItemService.requireItemForOwner(owner, ownedItemId).getStatus())
                .isEqualTo(OwnedItemStatus.ACTIVE);
    }

    private void assertThatTraded(AppUser owner, Long ownedItemId) {
        org.assertj.core.api.Assertions.assertThat(ownedItemService.requireItemForOwner(owner, ownedItemId).getStatus())
                .isEqualTo(OwnedItemStatus.TRADED);
    }

    private TradeCreateForm tradeForm() {
        TradeCreateForm form = new TradeCreateForm();
        form.setName("Controller Trade " + System.nanoTime());
        form.setOutgoingTradePercentage(new BigDecimal("100.00"));
        form.setIncomingTradePercentage(new BigDecimal("100.00"));
        return form;
    }

    private TradeOutgoingItemForm outgoingForm(Long ownedItemId, String agreedValue) {
        TradeOutgoingItemForm form = new TradeOutgoingItemForm();
        form.setOwnedItemId(ownedItemId);
        form.setOverrideValueSgd(new BigDecimal(agreedValue));
        return form;
    }

    private TradeIncomingItemForm incomingForm(Long cardId, String agreedValue) {
        TradeIncomingItemForm form = new TradeIncomingItemForm();
        form.setCardId(cardId);
        form.setVariant(CardVariant.STANDARD);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setOverrideValueSgd(new BigDecimal(agreedValue));
        return form;
    }

    private OwnedItem ownedItem(AppUser owner, String name, String purchasePrice) {
        Card card = card(name);
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(card.getId());
        form.setVariant(CardVariant.STANDARD);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        form.setPurchaseDate(LocalDate.of(2026, 6, 4));
        form.setGradedStatus(GradedStatus.UNGRADED);
        return ownedItemService.addCardToPortfolio(owner, form);
    }

    private Card card(String name) {
        CardForm form = new CardForm();
        form.setName(name + " " + System.nanoTime());
        form.setSetName("Trade Controller Set " + System.nanoTime());
        form.setCardNumber("TC-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
    }

    private Card verifiedCard(String name) {
        String slug = name.toLowerCase().replace(" ", "-");
        return cardService.importOfficialCard(new OfficialCardSearchResult(
                "trade-" + slug + "-" + System.nanoTime(),
                name,
                "trade-set",
                "Trade Controller Official Set",
                "Controller Series",
                LocalDate.of(2026, 1, 1),
                "199",
                "Illustration Rare",
                "https://images.example/" + slug + "-small.png",
                "https://images.example/" + slug + "-large.png",
                "https://cards.example/" + slug,
                LanguageMarket.ENGLISH,
                CatalogSource.POKEMON_TCG_API,
                List.of(CardVariant.STANDARD, CardVariant.HOLO, CardVariant.REVERSE_HOLO)));
    }

    private OwnedItemDisposalForm saleForm(String salePrice) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setSalePriceSgd(new BigDecimal(salePrice));
        form.setFeesSgd(BigDecimal.ZERO);
        return form;
    }

    private OwnedItemDisposalForm tradeAwayForm(String tradeValue) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setTradeValueSgd(new BigDecimal(tradeValue));
        return form;
    }

    private OwnedItemDisposalForm deleteForm() {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setNotes("Mistake");
        return form;
    }
}
