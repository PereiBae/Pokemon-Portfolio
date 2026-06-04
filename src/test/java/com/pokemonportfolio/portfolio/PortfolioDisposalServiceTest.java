package com.pokemonportfolio.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.DisposalType;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.repository.OwnedItemDisposalRepository;
import com.pokemonportfolio.portfolio.service.OwnedItemDisposalForm;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioDisposalService;
import com.pokemonportfolio.portfolio.service.PortfolioValuationService;
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
class PortfolioDisposalServiceTest {

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
    private PortfolioValuationService valuationService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Test
    void sellingItemCreatesDisposalAndCalculatesRealizedGainAfterFees() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, "Sell Service Card", "100.00");

        var disposal = disposalService.sellItem(owner, item.getId(), saleForm("140.00", "5.00"));

        assertThat(item.getStatus()).isEqualTo(OwnedItemStatus.SOLD);
        assertThat(disposal.getDisposalType()).isEqualTo(DisposalType.SOLD);
        assertThat(disposal.getNetProceedsSgd()).isEqualByComparingTo("135.00");
        assertThat(disposal.getRealizedGainLossSgd()).isEqualByComparingTo("35.00");
        assertThat(disposal.getRealizedGainLossPercent()).isEqualByComparingTo("35.0000");
        assertThat(disposalRepository.findByOwnedItemId(item.getId())).isPresent();
        assertThat(ownedItemService.listActiveItems(owner)).doesNotContain(item);
    }

    @Test
    void tradedItemCreatesDisposalAndCalculatesRealizedGain() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, "Trade Service Card", "80.00");

        var disposal = disposalService.tradeAwayItem(owner, item.getId(), tradeForm("95.00"));

        assertThat(item.getStatus()).isEqualTo(OwnedItemStatus.TRADED);
        assertThat(disposal.getDisposalType()).isEqualTo(DisposalType.TRADED);
        assertThat(disposal.getTradeTransactionId()).isNull();
        assertThat(disposal.getNetProceedsSgd()).isEqualByComparingTo("95.00");
        assertThat(disposal.getRealizedGainLossSgd()).isEqualByComparingTo("15.00");
        assertThat(ownedItemService.listActiveItems(owner)).doesNotContain(item);
    }

    @Test
    void deleteMistakeRemovesFromActiveHoldingsWithoutRealizedGain() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, "Delete Service Card", "90.00");

        var disposal = disposalService.deleteMistake(owner, item.getId(), deleteForm("Duplicate entry"));

        assertThat(item.getStatus()).isEqualTo(OwnedItemStatus.DELETED);
        assertThat(disposal.getDisposalType()).isEqualTo(DisposalType.DELETED);
        assertThat(disposal.getRealizedGainLossSgd()).isEqualByComparingTo("0.00");
        assertThat(disposalService.realizedSummary(owner).realizedGainLossSgd()).isEqualByComparingTo("0.00");
        assertThat(ownedItemService.listActiveItems(owner)).doesNotContain(item);
    }

    @Test
    void dashboardUsesActiveHoldingsForUnrealizedAndSoldTradedOnlyForRealized() {
        AppUser owner = owner();
        OwnedItem active = ownedItem(owner, "Active Performance Card", "50.00");
        priceSnapshot(active.getCard(), "70.00");
        OwnedItem sold = ownedItem(owner, "Sold Performance Card", "100.00");
        OwnedItem traded = ownedItem(owner, "Traded Performance Card", "80.00");
        OwnedItem deleted = ownedItem(owner, "Deleted Performance Card", "90.00");

        disposalService.sellItem(owner, sold.getId(), saleForm("130.00", "0.00"));
        disposalService.tradeAwayItem(owner, traded.getId(), tradeForm("75.00"));
        disposalService.deleteMistake(owner, deleted.getId(), deleteForm("Wrong copy"));

        var view = valuationService.calculateCurrentValue(owner);
        var snapshot = valuationService.createSnapshot(owner);

        assertThat(view.itemCount()).isEqualTo(1);
        assertThat(view.totalValueSgd()).isEqualByComparingTo("70.00");
        assertThat(view.totalCostBasisSgd()).isEqualByComparingTo("50.00");
        assertThat(view.unrealizedGainLossSgd()).isEqualByComparingTo("20.00");
        assertThat(view.realizedGainLossSgd()).isEqualByComparingTo("25.00");
        assertThat(view.realizedGainLossPercent()).isEqualByComparingTo("13.8889");
        assertThat(view.totalPerformanceSgd()).isEqualByComparingTo("45.00");
        assertThat(view.totalPerformancePercent()).isEqualByComparingTo("19.5652");
        assertThat(snapshot.getTotalValueSgd()).isEqualByComparingTo("70.00");
        assertThat(snapshot.getItemCount()).isEqualTo(1);
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
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
        form.setSetName("Disposal Service Set " + System.nanoTime());
        form.setCardNumber("DS-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
    }

    private OwnedItemDisposalForm saleForm(String salePrice, String fees) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setSalePriceSgd(new BigDecimal(salePrice));
        form.setFeesSgd(new BigDecimal(fees));
        form.setNotes("Sold locally");
        return form;
    }

    private OwnedItemDisposalForm tradeForm(String tradeValue) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setTradeValueSgd(new BigDecimal(tradeValue));
        form.setNotes("Traded locally");
        return form;
    }

    private OwnedItemDisposalForm deleteForm(String notes) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setNotes(notes);
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
                "Disposal valuation test price snapshot.",
                OffsetDateTime.now()));
    }
}
