package com.pokemonportfolio.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.pokemonportfolio.portfolio.repository.PortfolioValuationSnapshotRepository;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioValuationService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import com.pokemonportfolio.pricing.service.MarketValuationService;
import com.pokemonportfolio.trade.service.TradeCreateForm;
import com.pokemonportfolio.trade.service.TradeExecutionService;
import com.pokemonportfolio.trade.service.TradeIncomingItemForm;
import com.pokemonportfolio.trade.service.TradeOutgoingItemForm;
import com.pokemonportfolio.trade.service.TradeTransactionService;
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
class PortfolioValuationServiceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private MarketValuationService marketValuationService;

    @Autowired
    private PortfolioValuationService portfolioValuationService;

    @Autowired
    private PortfolioValuationSnapshotRepository valuationSnapshotRepository;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private TradeTransactionService tradeTransactionService;

    @Autowired
    private TradeExecutionService tradeExecutionService;

    @Test
    void calculatesCurrentPortfolioValueAndStoresHistoricalSnapshot() {
        var owner = appUserRepository.findByUsername("owner@example.com").orElseThrow();
        Card card = createCard();
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId()));
        var price = marketValuationService.refreshCardPrice(card);

        var view = portfolioValuationService.calculateCurrentValue(owner);
        var firstSnapshot = portfolioValuationService.createSnapshot(owner);
        var secondSnapshot = portfolioValuationService.createSnapshot(owner);

        assertThat(view.totalValueSgd()).isEqualByComparingTo(price.getMarketPriceSgd());
        assertThat(view.totalCostBasisSgd()).isEqualByComparingTo("50.00");
        assertThat(firstSnapshot.getId()).isNotEqualTo(secondSnapshot.getId());
        assertThat(valuationSnapshotRepository.findByOwnerOrderByCalculatedAtAscIdAsc(owner)).hasSize(2);
    }

    @Test
    void missingLatestPriceIsNotTreatedAsZeroInUnrealizedGainLoss() {
        var owner = appUserRepository.findByUsername("owner@example.com").orElseThrow();
        Card pricedCard = createCard("Priced Dashboard Card");
        Card unpricedCard = createCard("Unpriced Dashboard Card");
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(pricedCard.getId(), "50.00"));
        OwnedItem unpricedItem = ownedItemService.addCardToPortfolio(owner, ownedItemForm(unpricedCard.getId(), "125.00"));
        PriceSnapshot pricedSnapshot = marketValuationService.refreshCardPrice(pricedCard);

        var view = portfolioValuationService.calculateCurrentValue(owner);
        var unpricedView = view.items().stream()
                .filter(item -> item.ownedItemId().equals(unpricedItem.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(view.itemCount()).isEqualTo(2);
        assertThat(view.totalValueSgd()).isEqualByComparingTo(pricedSnapshot.getMarketPriceSgd());
        assertThat(view.totalCostBasisSgd()).isEqualByComparingTo("50.00");
        assertThat(view.unrealizedGainLossSgd())
                .isEqualByComparingTo(pricedSnapshot.getMarketPriceSgd().subtract(new BigDecimal("50.00")));
        assertThat(unpricedView.hasMarketValue()).isFalse();
        assertThat(unpricedView.marketValueSgd()).isNull();
        assertThat(unpricedView.gainLossSgd()).isNull();
        assertThat(unpricedView.confidenceRating()).isNull();
    }

    @Test
    void executedTradeKeepsDashboardRealizedAndUnrealizedGainLossAccurate() {
        var owner = appUserRepository.findByUsername("owner@example.com").orElseThrow();
        Card outgoingCard = createCard("Dashboard Trade Outgoing");
        Card incomingCard = createCard("Dashboard Trade Incoming");
        OwnedItem outgoing = ownedItemService.addCardToPortfolio(owner, ownedItemForm(outgoingCard.getId(), "200.00"));
        var trade = tradeTransactionService.createDraft(owner, tradeForm());
        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), "500.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incomingCard.getId(), "500.00"));

        tradeExecutionService.execute(owner, trade.getId());
        priceSnapshot(incomingCard, "650.00");

        var view = portfolioValuationService.calculateCurrentValue(owner);

        assertThat(view.itemCount()).isEqualTo(1);
        assertThat(view.totalCostBasisSgd()).isEqualByComparingTo("500.00");
        assertThat(view.totalValueSgd()).isEqualByComparingTo("650.00");
        assertThat(view.unrealizedGainLossSgd()).isEqualByComparingTo("150.00");
        assertThat(view.realizedGainLossSgd()).isEqualByComparingTo("300.00");
        assertThat(view.totalPerformanceSgd()).isEqualByComparingTo("450.00");
    }

    private Card createCard() {
        return createCard("Charizard");
    }

    private Card createCard(String name) {
        CardForm form = new CardForm();
        form.setName(name + " " + System.nanoTime());
        form.setSetName("VS1 Valuation Test " + System.nanoTime());
        form.setCardNumber("199-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.ALTERNATE_ART);
        return cardService.createManualCard(form);
    }

    private OwnedItemForm ownedItemForm(Long cardId) {
        return ownedItemForm(cardId, "50.00");
    }

    private OwnedItemForm ownedItemForm(Long cardId, String purchasePrice) {
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(cardId);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        form.setPurchaseDate(LocalDate.now());
        form.setGradedStatus(GradedStatus.UNGRADED);
        return form;
    }

    private TradeCreateForm tradeForm() {
        TradeCreateForm form = new TradeCreateForm();
        form.setName("Dashboard Trade " + System.nanoTime());
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
        form.setVariant(CardVariant.ALTERNATE_ART);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setOverrideValueSgd(new BigDecimal(agreedValue));
        return form;
    }

    private PriceSnapshot priceSnapshot(Card card, String marketPrice) {
        return priceSnapshotRepository.save(new PriceSnapshot(
                card,
                "MANUAL",
                new BigDecimal(marketPrice),
                "SGD",
                BigDecimal.ONE,
                new BigDecimal(marketPrice),
                ConfidenceRating.MEDIUM,
                "Trade dashboard compatibility snapshot.",
                OffsetDateTime.now()));
    }
}
