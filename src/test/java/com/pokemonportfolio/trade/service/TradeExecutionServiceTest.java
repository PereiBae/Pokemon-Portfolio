package com.pokemonportfolio.trade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.DisposalType;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.config.domain.TradeTransactionStatus;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.repository.OwnedItemDisposalRepository;
import com.pokemonportfolio.portfolio.service.OwnedItemDisposalForm;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioDisposalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TradeExecutionServiceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private PortfolioDisposalService disposalService;

    @Autowired
    private OwnedItemDisposalRepository disposalRepository;

    @Autowired
    private TradeTransactionService tradeTransactionService;

    @Autowired
    private TradeExecutionService tradeExecutionService;

    @Test
    void executingTradeMarksOutgoingCreatesDisposalAndCreatesIncomingWithCostBasis() {
        AppUser owner = owner();
        OwnedItem magikarp = ownedItem(owner, "Magikarp IR", "200.00");
        Card cardA = card("Incoming A");
        Card cardB = card("Incoming B");
        Card cardC = card("Incoming C");
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Magikarp Trade"));
        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(magikarp.getId(), "500.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(cardA.getId(), "250.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(cardB.getId(), "150.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(cardC.getId(), "100.00"));

        var executed = tradeExecutionService.execute(owner, trade.getId());
        var detail = tradeTransactionService.detailView(owner, trade.getId());
        var disposal = disposalRepository.findByOwnedItemId(magikarp.getId()).orElseThrow();

        assertThat(executed.getStatus()).isEqualTo(TradeTransactionStatus.EXECUTED);
        assertThat(magikarp.getStatus()).isEqualTo(OwnedItemStatus.TRADED);
        assertThat(disposal.getDisposalType()).isEqualTo(DisposalType.TRADED);
        assertThat(disposal.getTradeTransactionId()).isEqualTo(trade.getId());
        assertThat(disposal.getNetProceedsSgd()).isEqualByComparingTo("500.00");
        assertThat(disposal.getRealizedGainLossSgd()).isEqualByComparingTo("300.00");
        assertThat(detail.incomingItems()).extracting(TradeItemView::allocatedCostBasisSgd)
                .containsExactly(new BigDecimal("250.00"), new BigDecimal("150.00"), new BigDecimal("100.00"));
        assertThat(detail.incomingItems()).allSatisfy(item -> assertThat(item.incomingOwnedItemId()).isNotNull());
        assertThat(ownedItemService.listActiveItems(owner))
                .noneMatch(item -> item.getId().equals(magikarp.getId()))
                .filteredOn(item -> item.getNotes() != null && item.getNotes().contains("trade transaction #" + trade.getId()))
                .hasSize(3);
    }

    @Test
    void incomingCostBasisComesFromAdjustedIncomingValuesWhenTradeIsImbalanced() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Imbalanced Outgoing", "200.00");
        Card firstIncoming = card("Imbalanced Incoming One");
        Card secondIncoming = card("Imbalanced Incoming Two");
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Imbalanced Trade"));
        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), "400.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(firstIncoming.getId(), "250.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(secondIncoming.getId(), "250.00"));

        tradeExecutionService.execute(owner, trade.getId());
        var detail = tradeTransactionService.detailView(owner, trade.getId());
        var disposal = disposalRepository.findByOwnedItemId(outgoing.getId()).orElseThrow();

        assertThat(detail.tradeImbalanceSgd()).isEqualByComparingTo("100.00");
        assertThat(detail.incomingItems()).extracting(TradeItemView::allocatedCostBasisSgd)
                .containsExactly(new BigDecimal("250.00"), new BigDecimal("250.00"));
        assertThat(disposal.getNetProceedsSgd()).isEqualByComparingTo("500.00");
        assertThat(disposal.getRealizedGainLossSgd()).isEqualByComparingTo("300.00");
    }

    @Test
    void incomingTradeItemDoesNotCreateOwnedItemBeforeExecution() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Pre Execution Outgoing", "80.00");
        Card incoming = card("Pre Execution Incoming");
        var activeCountBefore = ownedItemService.listActiveItems(owner).size();
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Pre Execution Trade"));

        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), "120.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incoming.getId(), "120.00"));

        assertThat(ownedItemService.listActiveItems(owner)).hasSize(activeCountBefore);
        assertThat(ownedItemService.listActiveItems(owner)).contains(outgoing);
    }

    @Test
    void executionUsesIncomingAdjustedValuesForCostBasis() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Adjusted Basis Outgoing", "100.00");
        Card incoming = card("Adjusted Basis Incoming");
        TradeCreateForm tradeForm = tradeForm("Adjusted Basis Trade");
        tradeForm.setIncomingTradePercentage(new BigDecimal("80.00"));
        var trade = tradeTransactionService.createDraft(owner, tradeForm);
        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), "400.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incoming.getId(), "500.00"));

        tradeExecutionService.execute(owner, trade.getId());
        var detail = tradeTransactionService.detailView(owner, trade.getId());

        assertThat(detail.incomingItems().getFirst().adjustedValueSgd()).isEqualByComparingTo("400.00");
        assertThat(detail.incomingItems().getFirst().allocatedCostBasisSgd()).isEqualByComparingTo("400.00");
    }

    @Test
    void executingSameTradeTwiceIsPrevented() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Double Execute Outgoing", "50.00");
        Card incoming = card("Double Execute Incoming");
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Double Execute Trade"));
        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), "80.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incoming.getId(), "80.00"));

        tradeExecutionService.execute(owner, trade.getId());

        assertThatThrownBy(() -> tradeExecutionService.execute(owner, trade.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already been executed");
    }

    @Test
    void soldTradedAndDeletedItemsCannotBeAddedAsOutgoingTradeItems() {
        AppUser owner = owner();
        OwnedItem sold = ownedItem(owner, "Sold Excluded", "50.00");
        OwnedItem traded = ownedItem(owner, "Traded Excluded", "50.00");
        OwnedItem deleted = ownedItem(owner, "Deleted Excluded", "50.00");
        disposalService.sellItem(owner, sold.getId(), saleForm("60.00"));
        disposalService.tradeAwayItem(owner, traded.getId(), tradeAwayForm("65.00"));
        disposalService.deleteMistake(owner, deleted.getId(), deleteForm());
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Inactive Exclusion Trade"));

        assertThatThrownBy(() -> tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(sold.getId(), "60.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Portfolio item not found");
        assertThatThrownBy(() -> tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(traded.getId(), "65.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Portfolio item not found");
        assertThatThrownBy(() -> tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(deleted.getId(), "0.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Portfolio item not found");
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private TradeCreateForm tradeForm(String name) {
        TradeCreateForm form = new TradeCreateForm();
        form.setName(name + " " + System.nanoTime());
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
        form.setSetName("Trade Execution Set " + System.nanoTime());
        form.setCardNumber("TE-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
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
