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
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.config.domain.TradeFairnessResult;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.repository.OwnedItemDisposalRepository;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TradeAnalyzerServiceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private TradeTransactionService tradeTransactionService;

    @Autowired
    private TradeAnalyzerService tradeAnalyzerService;

    @Autowired
    private OwnedItemDisposalRepository disposalRepository;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Test
    void analysisOnlyCalculatesTotalsWithoutChangingPortfolio() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Analysis Outgoing", "100.00");
        Card incomingCard = card("Analysis Incoming");
        priceSnapshot(outgoing.getCard(), "125.00", ConfidenceRating.MEDIUM);
        priceSnapshot(incomingCard, "160.00", ConfidenceRating.HIGH);
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Analysis Trade", "80.00", "100.00"));
        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), "100.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incomingCard.getId(), "110.00"));

        var analysed = tradeAnalyzerService.analyze(owner, trade.getId());
        var view = tradeTransactionService.detailView(owner, trade.getId());

        assertThat(outgoing.getStatus()).isEqualTo(OwnedItemStatus.ACTIVE);
        assertThat(disposalRepository.findByOwnedItemId(outgoing.getId())).isEmpty();
        assertThat(ownedItemService.listActiveItems(owner)).contains(outgoing);
        assertThat(analysed.getTotalOutgoingMarketValueSgd()).isEqualByComparingTo("125.00");
        assertThat(analysed.getTotalIncomingMarketValueSgd()).isEqualByComparingTo("160.00");
        assertThat(analysed.getTotalOutgoingAdjustedValueSgd()).isEqualByComparingTo("80.00");
        assertThat(analysed.getTotalIncomingAdjustedValueSgd()).isEqualByComparingTo("110.00");
        assertThat(analysed.getNetDifferenceSgd()).isEqualByComparingTo("30.00");
        assertThat(analysed.getFairnessResult()).isEqualTo(TradeFairnessResult.FAVORABLE);
        assertThat(analysed.getConfidenceRating()).isEqualTo(ConfidenceRating.MEDIUM);
        assertThat(view.outgoingItems()).hasSize(1);
        assertThat(view.incomingItems()).hasSize(1);
    }

    @Test
    void latestMarketValueIsUsedWhenOverrideValueIsBlank() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Latest Market Outgoing", "300.00");
        Card incomingCard = card("Latest Market Incoming");
        priceSnapshot(outgoing.getCard(), "500.00", ConfidenceRating.MEDIUM);
        priceSnapshot(incomingCard, "400.00", ConfidenceRating.HIGH);
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Latest Market Trade", "80.00", "100.00"));

        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), null));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incomingCard.getId(), null));

        var analysed = tradeAnalyzerService.analyze(owner, trade.getId());
        var view = tradeTransactionService.detailView(owner, trade.getId());

        assertThat(analysed.getTotalOutgoingAgreedValueSgd()).isEqualByComparingTo("500.00");
        assertThat(analysed.getTotalOutgoingAdjustedValueSgd()).isEqualByComparingTo("400.00");
        assertThat(analysed.getTotalIncomingAgreedValueSgd()).isEqualByComparingTo("400.00");
        assertThat(analysed.getTotalIncomingAdjustedValueSgd()).isEqualByComparingTo("400.00");
        assertThat(analysed.getNetDifferenceSgd()).isEqualByComparingTo("0.00");
        assertThat(view.outgoingItems().getFirst().overrideValueSgd()).isNull();
        assertThat(view.outgoingItems().getFirst().baseValueSgd()).isEqualByComparingTo("500.00");
    }

    @Test
    void overrideValueIsUsedWhenProvided() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Override Outgoing", "300.00");
        Card incomingCard = card("Override Incoming");
        priceSnapshot(outgoing.getCard(), "500.00", ConfidenceRating.MEDIUM);
        priceSnapshot(incomingCard, "400.00", ConfidenceRating.HIGH);
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Override Trade", "80.00", "100.00"));

        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), "600.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incomingCard.getId(), "350.00"));

        var analysed = tradeAnalyzerService.analyze(owner, trade.getId());
        var view = tradeTransactionService.detailView(owner, trade.getId());

        assertThat(analysed.getTotalOutgoingMarketValueSgd()).isEqualByComparingTo("500.00");
        assertThat(analysed.getTotalOutgoingAgreedValueSgd()).isEqualByComparingTo("600.00");
        assertThat(analysed.getTotalOutgoingAdjustedValueSgd()).isEqualByComparingTo("480.00");
        assertThat(analysed.getTotalIncomingMarketValueSgd()).isEqualByComparingTo("400.00");
        assertThat(analysed.getTotalIncomingAgreedValueSgd()).isEqualByComparingTo("350.00");
        assertThat(analysed.getTotalIncomingAdjustedValueSgd()).isEqualByComparingTo("350.00");
        assertThat(view.outgoingItems().getFirst().overrideValueSgd()).isEqualByComparingTo("600.00");
        assertThat(view.outgoingItems().getFirst().baseValueSgd()).isEqualByComparingTo("600.00");
    }

    @Test
    void missingMarketValueWithoutOverrideReturnsValidationError() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Missing Market Outgoing", "80.00");
        Card incomingCard = card("Missing Market Incoming");
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Missing Market Trade", "100.00", "100.00"));

        assertThatThrownBy(() -> tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No market value found. Add a manual price entry or provide an override value.");
        assertThatThrownBy(() -> tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incomingCard.getId(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No market value found for this incoming card. Enter an Override Value SGD to continue.");
    }

    @Test
    void nullIncomingCardSelectionReturnsValidationError() {
        AppUser owner = owner();
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Null Incoming Trade", "100.00", "100.00"));
        TradeIncomingItemForm form = incomingForm(null, "100.00");

        assertThatThrownBy(() -> tradeTransactionService.addIncomingItem(owner, trade.getId(), form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please select an incoming card or sealed product.");
    }

    @Test
    void fairnessIsBalancedWhenAdjustedDifferenceIsWithinTolerance() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedItem(owner, "Balanced Outgoing", "80.00");
        Card incomingCard = card("Balanced Incoming");
        var trade = tradeTransactionService.createDraft(owner, tradeForm("Balanced Trade", "100.00", "100.00"));
        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), "100.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incomingCard.getId(), "104.00"));

        var analysed = tradeAnalyzerService.analyze(owner, trade.getId());

        assertThat(analysed.getNetDifferenceSgd()).isEqualByComparingTo("4.00");
        assertThat(analysed.getFairnessResult()).isEqualTo(TradeFairnessResult.BALANCED);
        assertThat(analysed.getConfidenceRating()).isEqualTo(ConfidenceRating.LOW);
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private TradeCreateForm tradeForm(String name, String outgoingPercentage, String incomingPercentage) {
        TradeCreateForm form = new TradeCreateForm();
        form.setName(name + " " + System.nanoTime());
        form.setOutgoingTradePercentage(new BigDecimal(outgoingPercentage));
        form.setIncomingTradePercentage(new BigDecimal(incomingPercentage));
        return form;
    }

    private TradeOutgoingItemForm outgoingForm(Long ownedItemId, String agreedValue) {
        TradeOutgoingItemForm form = new TradeOutgoingItemForm();
        form.setOwnedItemId(ownedItemId);
        if (agreedValue != null) {
            form.setOverrideValueSgd(new BigDecimal(agreedValue));
        }
        return form;
    }

    private TradeIncomingItemForm incomingForm(Long cardId, String agreedValue) {
        TradeIncomingItemForm form = new TradeIncomingItemForm();
        form.setCardId(cardId);
        form.setVariant(CardVariant.STANDARD);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        if (agreedValue != null) {
            form.setOverrideValueSgd(new BigDecimal(agreedValue));
        }
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
        form.setSetName("Trade Analyzer Set " + System.nanoTime());
        form.setCardNumber("TA-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
    }

    private PriceSnapshot priceSnapshot(Card card, String marketPrice, ConfidenceRating confidenceRating) {
        return priceSnapshotRepository.save(new PriceSnapshot(
                card,
                "MANUAL",
                new BigDecimal(marketPrice),
                "SGD",
                BigDecimal.ONE,
                new BigDecimal(marketPrice),
                confidenceRating,
                "Trade analyzer test snapshot.",
                OffsetDateTime.now()));
    }
}
