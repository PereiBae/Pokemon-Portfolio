package com.pokemonportfolio.grading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.SealedProductForm;
import com.pokemonportfolio.catalog.service.SealedProductService;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.GradingRecommendation;
import com.pokemonportfolio.config.domain.GradingScenarioType;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.SealedProductCondition;
import com.pokemonportfolio.config.domain.SealedProductType;
import com.pokemonportfolio.grading.repository.GradingAnalysisRepository;
import com.pokemonportfolio.grading.repository.GradingScenarioRepository;
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
class GradingAnalyzerServiceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private SealedProductService sealedProductService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private PortfolioDisposalService disposalService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private GradingFeeService gradingFeeService;

    @Autowired
    private GradingAnalyzerService gradingAnalyzerService;

    @Autowired
    private GradingAnalysisRepository gradingAnalysisRepository;

    @Autowired
    private GradingScenarioRepository gradingScenarioRepository;

    @Test
    void activeUngradedCardsAreEligibleButSealedDisposedAndGradedItemsAreExcluded() {
        AppUser owner = owner();
        OwnedItem activeUngraded = ownedCard(owner, "Eligible Grading Card", "100.00", GradedStatus.UNGRADED, null);
        ownedSealedProduct(owner);
        OwnedItem sold = ownedCard(owner, "Sold Grading Card", "100.00", GradedStatus.UNGRADED, null);
        OwnedItem traded = ownedCard(owner, "Traded Grading Card", "100.00", GradedStatus.UNGRADED, null);
        OwnedItem deleted = ownedCard(owner, "Deleted Grading Card", "100.00", GradedStatus.UNGRADED, null);
        ownedCard(owner, "Already PSA Graded Card", "100.00", GradedStatus.PSA_GRADED, 9);

        disposalService.sellItem(owner, sold.getId(), saleForm("110.00"));
        disposalService.tradeAwayItem(owner, traded.getId(), tradeForm("120.00"));
        disposalService.deleteMistake(owner, deleted.getId(), deleteForm());

        var eligibleItems = gradingAnalyzerService.eligibleItems(owner);

        assertThat(eligibleItems).extracting(GradingEligibleItemView::ownedItemId)
                .contains(activeUngraded.getId())
                .doesNotContain(sold.getId(), traded.getId(), deleted.getId());
        assertThat(eligibleItems).extracting(GradingEligibleItemView::displayLabel)
                .anyMatch(label -> label.contains("Eligible Grading Card"))
                .noneMatch(label -> label.contains("Already PSA Graded Card"))
                .noneMatch(label -> label.contains("Sealed"));
    }

    @Test
    void alreadyGradedCardCannotBeAnalysed() {
        AppUser owner = owner();
        OwnedItem gradedCard = ownedCard(owner, "Rejected Graded Card", "100.00", GradedStatus.PSA_GRADED, 9);

        assertThatThrownBy(() -> gradingAnalyzerService.analyze(owner, analysisForm(
                        gradedCard.getId(),
                        "120.00",
                        "130.00",
                        "160.00",
                        "230.00",
                        "0.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already graded");
    }

    @Test
    void formPrefillsRawValueFromLatestPriceSnapshot() {
        AppUser owner = owner();
        OwnedItem item = ownedCard(owner, "Prefill Raw Grading Card", "100.00", GradedStatus.UNGRADED, null);
        snapshot(item.getCard(), "120.00", OffsetDateTime.now().minusDays(1), ConfidenceRating.LOW);
        snapshot(item.getCard(), "140.00", OffsetDateTime.now(), ConfidenceRating.MEDIUM);

        GradingAnalysisForm form = gradingAnalyzerService.formFor(owner, item.getId());

        assertThat(form.getRawValueSgd()).isEqualByComparingTo("140.00");
        assertThat(form.getGradingFeeId()).isNotNull();
        assertThat(form.getEstimatedTurnaroundDays()).isEqualTo(65);
    }

    @Test
    void calculatesConservativeBalancedAndAggressiveScenariosAndPersistsResult() {
        AppUser owner = owner();
        OwnedItem item = ownedCard(owner, "Calculated Grading Card", "100.00", GradedStatus.UNGRADED, null);
        snapshot(item.getCard(), "125.00", OffsetDateTime.now(), ConfidenceRating.MEDIUM);

        GradingAnalysisView analysis = gradingAnalyzerService.analyze(owner, analysisForm(
                item.getId(),
                "125.00",
                "120.00",
                "205.00",
                "300.00",
                "15.00"));

        assertThat(analysis.recommendation()).isEqualTo(GradingRecommendation.STRONG_GRADE);
        assertThat(analysis.gradingFeeSgd()).isEqualByComparingTo("35.00");
        assertThat(analysis.opportunityCostSgd()).isEqualByComparingTo("15.00");
        assertThat(analysis.confidenceRating()).isEqualTo(ConfidenceRating.MEDIUM);
        assertThat(analysis.scenarios()).hasSize(3);

        GradingScenarioView conservative = scenario(analysis, GradingScenarioType.CONSERVATIVE);
        GradingScenarioView balanced = scenario(analysis, GradingScenarioType.BALANCED);
        GradingScenarioView aggressive = scenario(analysis, GradingScenarioType.AGGRESSIVE);

        assertThat(conservative.totalCostSgd()).isEqualByComparingTo("150.00");
        assertThat(conservative.expectedProfitSgd()).isEqualByComparingTo("-30.00");
        assertThat(conservative.roiPercentage()).isEqualByComparingTo("-20.0000");
        assertThat(conservative.warningMessage())
                .isEqualTo("Raw value is higher than PSA 8 value. Grading downside risk is high.");

        assertThat(balanced.expectedProfitSgd()).isEqualByComparingTo("55.00");
        assertThat(balanced.roiPercentage()).isEqualByComparingTo("36.6667");
        assertThat(balanced.recommendation()).isEqualTo(GradingRecommendation.STRONG_GRADE);

        assertThat(aggressive.expectedProfitSgd()).isEqualByComparingTo("150.00");
        assertThat(gradingAnalysisRepository.findByOwnerOrderByCreatedAtDesc(owner)).hasSize(1);
        assertThat(gradingScenarioRepository.findAll()).hasSize(3);
    }

    @Test
    void expectedProfitAtSgd50MeetsProfitThreshold() {
        AppUser owner = owner();
        OwnedItem item = ownedCard(owner, "Threshold Grading Card", "100.00", GradedStatus.UNGRADED, null);

        GradingAnalysisView analysis = gradingAnalyzerService.analyze(owner, analysisForm(
                item.getId(),
                "120.00",
                "140.00",
                "185.00",
                "160.00",
                "0.00"));

        GradingScenarioView balanced = scenario(analysis, GradingScenarioType.BALANCED);
        assertThat(balanced.expectedProfitSgd()).isEqualByComparingTo("50.00");
        assertThat(balanced.recommendation()).isEqualTo(GradingRecommendation.STRONG_GRADE);
        assertThat(analysis.recommendation()).isEqualTo(GradingRecommendation.STRONG_GRADE);
    }

    @Test
    void rawValueWarningsDriveDoNotGradeWhenPsa8AndPsa9UnderperformRaw() {
        AppUser owner = owner();
        OwnedItem item = ownedCard(owner, "Raw Beats Graded Card", "100.00", GradedStatus.UNGRADED, null);

        GradingAnalysisView analysis = gradingAnalyzerService.analyze(owner, analysisForm(
                item.getId(),
                "150.00",
                "120.00",
                "140.00",
                "145.00",
                "0.00"));

        assertThat(scenario(analysis, GradingScenarioType.CONSERVATIVE).warningMessage())
                .isEqualTo("Raw value is higher than PSA 8 value. Grading downside risk is high.");
        assertThat(scenario(analysis, GradingScenarioType.BALANCED).warningMessage())
                .isEqualTo("Raw value is higher than PSA 9 value. PSA 9 may underperform raw.");
        assertThat(analysis.recommendation()).isEqualTo(GradingRecommendation.DO_NOT_GRADE);
    }

    @Test
    void psa10OnlyProfitabilityShowsDependencyWarning() {
        AppUser owner = owner();
        OwnedItem item = ownedCard(owner, "PSA 10 Dependency Card", "100.00", GradedStatus.UNGRADED, null);

        GradingAnalysisView analysis = gradingAnalyzerService.analyze(owner, analysisForm(
                item.getId(),
                "120.00",
                "140.00",
                "150.00",
                "190.00",
                "0.00"));

        GradingScenarioView aggressive = scenario(analysis, GradingScenarioType.AGGRESSIVE);
        assertThat(aggressive.expectedProfitSgd()).isEqualByComparingTo("55.00");
        assertThat(aggressive.warningMessage())
                .isEqualTo("This grading case depends heavily on achieving PSA 10.");
        assertThat(aggressive.recommendation()).isEqualTo(GradingRecommendation.GRADE_ONLY_IF_CONFIDENT_PSA10);
        assertThat(analysis.recommendation()).isEqualTo(GradingRecommendation.GRADE_ONLY_IF_CONFIDENT_PSA10);
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private OwnedItem ownedCard(
            AppUser owner,
            String name,
            String purchasePrice,
            GradedStatus gradedStatus,
            Integer psaGrade) {
        Card card = card(name);
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(card.getId());
        form.setVariant(CardVariant.STANDARD);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        form.setPurchaseDate(LocalDate.of(2026, 6, 4));
        form.setGradedStatus(gradedStatus);
        form.setPsaGrade(psaGrade);
        return ownedItemService.addCardToPortfolio(owner, form);
    }

    private OwnedItem ownedSealedProduct(AppUser owner) {
        SealedProductForm sealedForm = new SealedProductForm();
        sealedForm.setName("Grading Sealed Product " + System.nanoTime());
        sealedForm.setProductType(SealedProductType.BOOSTER_BOX);
        sealedForm.setLanguageMarket(LanguageMarket.ENGLISH);
        sealedForm.setSetName("Grading Sealed Set");
        SealedProduct product = sealedProductService.createManualSealedProduct(sealedForm);

        OwnedItemForm ownedItemForm = new OwnedItemForm();
        ownedItemForm.setSealedProductId(product.getId());
        ownedItemForm.setSealedCondition(SealedProductCondition.SEALED);
        ownedItemForm.setPurchasePriceSgd(new BigDecimal("180.00"));
        ownedItemForm.setPurchaseDate(LocalDate.of(2026, 6, 4));
        return ownedItemService.addSealedProductToPortfolio(owner, ownedItemForm);
    }

    private Card card(String name) {
        CardForm form = new CardForm();
        form.setName(name);
        form.setSetName("Grading Service Set " + System.nanoTime());
        form.setCardNumber("GA-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
    }

    private PriceSnapshot snapshot(
            Card card,
            String marketPrice,
            OffsetDateTime calculatedAt,
            ConfidenceRating confidenceRating) {
        return priceSnapshotRepository.save(new PriceSnapshot(
                card,
                "MANUAL",
                new BigDecimal(marketPrice),
                "SGD",
                BigDecimal.ONE,
                new BigDecimal(marketPrice),
                confidenceRating,
                "Grading test price snapshot.",
                calculatedAt));
    }

    private GradingAnalysisForm analysisForm(
            Long ownedItemId,
            String rawValue,
            String psa8Value,
            String psa9Value,
            String psa10Value,
            String opportunityCost) {
        GradingAnalysisForm form = new GradingAnalysisForm();
        form.setOwnedItemId(ownedItemId);
        form.setRawValueSgd(new BigDecimal(rawValue));
        form.setPsa8ValueSgd(new BigDecimal(psa8Value));
        form.setPsa9ValueSgd(new BigDecimal(psa9Value));
        form.setPsa10ValueSgd(new BigDecimal(psa10Value));
        form.setGradingFeeId(gradingFeeService.defaultPsaFee().getId());
        form.setEstimatedTurnaroundDays(65);
        form.setOpportunityCostSgd(new BigDecimal(opportunityCost));
        return form;
    }

    private GradingScenarioView scenario(GradingAnalysisView analysis, GradingScenarioType scenarioType) {
        return analysis.scenarios().stream()
                .filter(scenario -> scenario.scenarioType() == scenarioType)
                .findFirst()
                .orElseThrow();
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

    private OwnedItemDisposalForm deleteForm() {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        return form;
    }
}
