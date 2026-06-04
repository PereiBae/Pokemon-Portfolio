package com.pokemonportfolio.alerts.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.alerts.repository.AlertRepository;
import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.AlertStatus;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.service.OwnedItemDisposalForm;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioDisposalService;
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
class PriceAlertServiceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private PriceAlertService priceAlertService;

    @Autowired
    private AlertViewService alertViewService;

    @Autowired
    private PortfolioDisposalService disposalService;

    @Test
    void itemPurchasedBelowSgd100TriggersAtSgd10Gain() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, new BigDecimal("80.00"));
        snapshot(item.getCard(), "90.00", OffsetDateTime.now());

        var created = priceAlertService.checkAlerts(owner);

        assertThat(created).hasSize(1);
        assertThat(created.getFirst().getGainAmountSgd()).isEqualByComparingTo("10.00");
    }

    @Test
    void itemPurchasedBelowSgd100DoesNotTriggerBelowSgd10Gain() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, new BigDecimal("80.00"));
        snapshot(item.getCard(), "89.99", OffsetDateTime.now());

        assertThat(priceAlertService.checkAlerts(owner)).isEmpty();
    }

    @Test
    void itemPurchasedAtOrAboveSgd100TriggersAtSgd25Gain() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, new BigDecimal("120.00"));
        snapshot(item.getCard(), "145.00", OffsetDateTime.now());

        var created = priceAlertService.checkAlerts(owner);

        assertThat(created).hasSize(1);
        assertThat(created.getFirst().getGainAmountSgd()).isEqualByComparingTo("25.00");
    }

    @Test
    void itemPurchasedAtOrAboveSgd100DoesNotTriggerBelowSgd25Gain() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, new BigDecimal("120.00"));
        snapshot(item.getCard(), "140.00", OffsetDateTime.now());

        assertThat(priceAlertService.checkAlerts(owner)).isEmpty();
    }

    @Test
    void calculatesGainPercentage() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, new BigDecimal("80.00"));
        snapshot(item.getCard(), "92.00", OffsetDateTime.now());

        var created = priceAlertService.checkAlerts(owner);

        assertThat(created.getFirst().getGainPercentage()).isEqualByComparingTo("15.0000");
    }

    @Test
    void duplicateAlertsAreNotCreatedForSameOwnedItemAndSnapshot() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, new BigDecimal("80.00"));
        snapshot(item.getCard(), "91.00", OffsetDateTime.now());

        priceAlertService.checkAlerts(owner);
        priceAlertService.checkAlerts(owner);

        assertThat(alertRepository.findByOwnerOrderByTriggeredAtDescIdDesc(owner)).hasSize(1);
    }

    @Test
    void dismissedAlertIsRetainedInHistory() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, new BigDecimal("80.00"));
        snapshot(item.getCard(), "91.00", OffsetDateTime.now());
        Long alertId = priceAlertService.checkAlerts(owner).getFirst().getId();

        alertViewService.dismissAlert(owner, alertId);
        AlertPageView page = alertViewService.pageFor(owner);

        assertThat(page.activeAlerts()).isEmpty();
        assertThat(page.historicalAlerts()).hasSize(1);
        assertThat(page.historicalAlerts().getFirst().status()).isEqualTo(AlertStatus.DISMISSED);
    }

    @Test
    void alertGenerationUsesLatestPriceSnapshot() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, new BigDecimal("80.00"));
        snapshot(item.getCard(), "95.00", OffsetDateTime.now().minusDays(1));
        snapshot(item.getCard(), "91.00", OffsetDateTime.now());

        var created = priceAlertService.checkAlerts(owner);

        assertThat(created).hasSize(1);
        assertThat(created.getFirst().getCurrentMarketValueSgd()).isEqualByComparingTo("91.00");
    }

    @Test
    void disposedItemsDoNotTriggerAlerts() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, new BigDecimal("80.00"));
        snapshot(item.getCard(), "120.00", OffsetDateTime.now());
        disposalService.tradeAwayItem(owner, item.getId(), tradeForm("90.00"));

        assertThat(priceAlertService.checkAlerts(owner)).isEmpty();
    }

    @Test
    void disposingItemDismissesExistingActiveAlertButRetainsHistory() {
        AppUser owner = owner();
        OwnedItem item = ownedItem(owner, new BigDecimal("80.00"));
        snapshot(item.getCard(), "91.00", OffsetDateTime.now());
        priceAlertService.checkAlerts(owner);

        disposalService.sellItem(owner, item.getId(), saleForm("100.00"));
        AlertPageView page = alertViewService.pageFor(owner);

        assertThat(page.activeAlerts()).isEmpty();
        assertThat(page.historicalAlerts()).hasSize(1);
        assertThat(page.historicalAlerts().getFirst().status()).isEqualTo(AlertStatus.DISMISSED);
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private OwnedItem ownedItem(AppUser owner, BigDecimal purchasePrice) {
        Card card = card();
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(card.getId());
        form.setVariant(CardVariant.STANDARD);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(purchasePrice);
        form.setPurchaseDate(LocalDate.of(2026, 6, 3));
        form.setGradedStatus(GradedStatus.UNGRADED);
        return ownedItemService.addCardToPortfolio(owner, form);
    }

    private Card card() {
        CardForm form = new CardForm();
        form.setName("Alert Test Card " + System.nanoTime());
        form.setSetName("Alert Test Set " + System.nanoTime());
        form.setCardNumber("AT-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
    }

    private PriceSnapshot snapshot(Card card, String marketPrice, OffsetDateTime calculatedAt) {
        return priceSnapshotRepository.save(new PriceSnapshot(
                card,
                "MANUAL",
                new BigDecimal(marketPrice),
                "SGD",
                BigDecimal.ONE,
                new BigDecimal(marketPrice),
                ConfidenceRating.MEDIUM,
                "Test price snapshot.",
                calculatedAt));
    }

    private OwnedItemDisposalForm saleForm(String salePrice) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setSalePriceSgd(new BigDecimal(salePrice));
        form.setFeesSgd(BigDecimal.ZERO);
        return form;
    }

    private OwnedItemDisposalForm tradeForm(String tradeValue) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setTradeValueSgd(new BigDecimal(tradeValue));
        return form;
    }
}
