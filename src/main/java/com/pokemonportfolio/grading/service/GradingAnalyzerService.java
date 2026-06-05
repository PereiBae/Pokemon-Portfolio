package com.pokemonportfolio.grading.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.GradingRecommendation;
import com.pokemonportfolio.config.domain.GradingScenarioType;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.config.domain.PsaGrade;
import com.pokemonportfolio.grading.entity.GradingAnalysis;
import com.pokemonportfolio.grading.entity.GradingFee;
import com.pokemonportfolio.grading.entity.GradingScenario;
import com.pokemonportfolio.grading.repository.GradingAnalysisRepository;
import com.pokemonportfolio.grading.repository.GradingScenarioRepository;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.repository.OwnedItemRepository;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingAnalyzerService {

    private static final BigDecimal DEFAULT_MINIMUM_PROFIT_THRESHOLD_SGD = new BigDecimal("50.00");

    private final OwnedItemRepository ownedItemRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final GradingFeeService gradingFeeService;
    private final GradingAnalysisRepository gradingAnalysisRepository;
    private final GradingScenarioRepository gradingScenarioRepository;

    public GradingAnalyzerService(
            OwnedItemRepository ownedItemRepository,
            PriceSnapshotRepository priceSnapshotRepository,
            GradingFeeService gradingFeeService,
            GradingAnalysisRepository gradingAnalysisRepository,
            GradingScenarioRepository gradingScenarioRepository) {
        this.ownedItemRepository = ownedItemRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.gradingFeeService = gradingFeeService;
        this.gradingAnalysisRepository = gradingAnalysisRepository;
        this.gradingScenarioRepository = gradingScenarioRepository;
    }

    @Transactional(readOnly = true)
    public List<GradingEligibleItemView> eligibleItems(AppUser owner) {
        return ownedItemRepository.findByOwnerAndStatusOrderByCreatedAtDesc(owner, OwnedItemStatus.ACTIVE).stream()
                .filter(this::isEligibleForGrading)
                .map(item -> GradingEligibleItemView.from(item, latestRawValue(item).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public GradingAnalysisForm formFor(AppUser owner, Long ownedItemId) {
        GradingAnalysisForm form = new GradingAnalysisForm();
        if (ownedItemId != null) {
            OwnedItem item = requireActiveCardForAnalysis(owner, ownedItemId);
            form.setOwnedItemId(item.getId());
            latestRawValue(item).ifPresent(form::setRawValueSgd);
        }
        GradingFee fee = gradingFeeService.defaultPsaFee();
        form.setGradingFeeId(fee.getId());
        form.setEstimatedTurnaroundDays(fee.getEstimatedTurnaroundDays());
        return form;
    }

    @Transactional
    public GradingAnalysisView analyze(AppUser owner, GradingAnalysisForm form) {
        OwnedItem item = requireActiveCardForAnalysis(owner, form.getOwnedItemId());
        GradingFee fee = gradingFeeService.requireActiveFee(form.getGradingFeeId());
        BigDecimal rawValue = moneyOrLatestRawValue(item, form.getRawValueSgd());
        BigDecimal psa8Value = requiredNonNegativeMoney(form.getPsa8ValueSgd(), "PSA 8 value is required");
        BigDecimal psa9Value = requiredNonNegativeMoney(form.getPsa9ValueSgd(), "PSA 9 value is required");
        BigDecimal psa10Value = requiredNonNegativeMoney(form.getPsa10ValueSgd(), "PSA 10 value is required");
        BigDecimal opportunityCost = optionalNonNegativeMoney(form.getOpportunityCostSgd());
        BigDecimal gradingFee = MoneyCalculationSupport.money(fee.getFeeSgd());
        int turnaroundDays = form.getEstimatedTurnaroundDays() == null
                ? fee.getEstimatedTurnaroundDays()
                : form.getEstimatedTurnaroundDays();
        if (turnaroundDays <= 0) {
            throw new IllegalArgumentException("Turnaround days must be positive");
        }
        BigDecimal totalCost = MoneyCalculationSupport.money(
                item.getPurchasePriceSgd().add(gradingFee).add(opportunityCost));
        ScenarioCalculation conservative = scenario(
                GradingScenarioType.CONSERVATIVE,
                PsaGrade.PSA_8,
                psa8Value,
                totalCost,
                rawValue,
                DEFAULT_MINIMUM_PROFIT_THRESHOLD_SGD);
        ScenarioCalculation balanced = scenario(
                GradingScenarioType.BALANCED,
                PsaGrade.PSA_9,
                psa9Value,
                totalCost,
                rawValue,
                DEFAULT_MINIMUM_PROFIT_THRESHOLD_SGD);
        ScenarioCalculation aggressive = scenario(
                GradingScenarioType.AGGRESSIVE,
                PsaGrade.PSA_10,
                psa10Value,
                totalCost,
                rawValue,
                DEFAULT_MINIMUM_PROFIT_THRESHOLD_SGD);

        boolean psa10OnlyProfitable = !conservative.profitable()
                && !balanced.profitable()
                && aggressive.profitable();
        if (psa10OnlyProfitable) {
            aggressive = aggressive.withWarning("This grading case depends heavily on achieving PSA 10.");
        }
        GradingRecommendation finalRecommendation = finalRecommendation(
                conservative,
                balanced,
                aggressive,
                rawValue,
                psa8Value,
                psa9Value);
        ConfidenceRating confidence = latestSnapshot(item)
                .map(PriceSnapshot::getConfidenceRating)
                .orElse(ConfidenceRating.LOW);
        GradingAnalysis analysis = gradingAnalysisRepository.save(new GradingAnalysis(
                owner,
                item,
                fee,
                rawValue,
                psa8Value,
                psa9Value,
                psa10Value,
                gradingFee,
                turnaroundDays,
                opportunityCost,
                DEFAULT_MINIMUM_PROFIT_THRESHOLD_SGD,
                finalRecommendation,
                confidence,
                blankToNull(form.getNotes())));
        List<GradingScenario> scenarios = gradingScenarioRepository.saveAll(List.of(
                conservative.toEntity(analysis),
                balanced.toEntity(analysis),
                aggressive.toEntity(analysis)));
        return GradingAnalysisView.from(
                analysis,
                scenarios.stream()
                        .sorted(Comparator.comparing(GradingScenario::getId))
                        .map(GradingScenarioView::from)
                        .toList());
    }

    @Transactional(readOnly = true)
    public GradingAnalysisView view(AppUser owner, Long analysisId) {
        GradingAnalysis analysis = gradingAnalysisRepository.findByIdAndOwner(analysisId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Grading analysis not found"));
        List<GradingScenarioView> scenarios = gradingScenarioRepository
                .findByGradingAnalysisOrderByIdAsc(analysis)
                .stream()
                .map(GradingScenarioView::from)
                .toList();
        return GradingAnalysisView.from(analysis, scenarios);
    }

    @Transactional(readOnly = true)
    public List<GradingAnalysisListView> latestAnalyses(AppUser owner, int limit) {
        return gradingAnalysisRepository.findByOwnerOrderByCreatedAtDesc(owner).stream()
                .limit(limit)
                .map(GradingAnalysisListView::from)
                .toList();
    }

    private boolean isEligibleForGrading(OwnedItem item) {
        return item.isCard()
                && item.getStatus() == OwnedItemStatus.ACTIVE
                && item.getGradedStatus() == GradedStatus.UNGRADED;
    }

    private OwnedItem requireActiveCardForAnalysis(AppUser owner, Long ownedItemId) {
        if (ownedItemId == null) {
            throw new IllegalArgumentException("Select an active ungraded card");
        }
        OwnedItem item = ownedItemRepository.findByIdAndOwner(ownedItemId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio item not found"));
        if (item.getStatus() != OwnedItemStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active cards can be analysed for grading");
        }
        if (!item.isCard()) {
            throw new IllegalArgumentException("Sealed products are not eligible for PSA grading analysis");
        }
        if (item.getGradedStatus() != GradedStatus.UNGRADED) {
            throw new IllegalArgumentException("This card is already graded");
        }
        return item;
    }

    private Optional<BigDecimal> latestRawValue(OwnedItem item) {
        return latestSnapshot(item)
                .map(PriceSnapshot::getMarketPriceSgd)
                .map(MoneyCalculationSupport::money);
    }

    private Optional<PriceSnapshot> latestSnapshot(OwnedItem item) {
        return priceSnapshotRepository.findTopByCardIdOrderByCalculatedAtDescIdDesc(item.getCard().getId());
    }

    private BigDecimal moneyOrLatestRawValue(OwnedItem item, BigDecimal formValue) {
        if (formValue != null) {
            return requiredNonNegativeMoney(formValue, "Raw value is required");
        }
        return latestRawValue(item)
                .orElseThrow(() -> new IllegalArgumentException("Raw value is required when no price snapshot exists"));
    }

    private BigDecimal requiredNonNegativeMoney(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        BigDecimal money = MoneyCalculationSupport.money(value);
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
        return money;
    }

    private BigDecimal optionalNonNegativeMoney(BigDecimal value) {
        BigDecimal money = MoneyCalculationSupport.money(value);
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Opportunity cost must be non-negative");
        }
        return money;
    }

    private ScenarioCalculation scenario(
            GradingScenarioType type,
            PsaGrade grade,
            BigDecimal expectedValue,
            BigDecimal totalCost,
            BigDecimal rawValue,
            BigDecimal threshold) {
        BigDecimal expectedProfit = MoneyCalculationSupport.money(expectedValue.subtract(totalCost));
        BigDecimal roi = MoneyCalculationSupport.percent(expectedProfit, totalCost);
        boolean profitable = expectedProfit.compareTo(threshold) >= 0;
        String warning = null;
        if (grade == PsaGrade.PSA_8 && rawValue.compareTo(expectedValue) > 0) {
            warning = "Raw value is higher than PSA 8 value. Grading downside risk is high.";
        }
        if (grade == PsaGrade.PSA_9 && rawValue.compareTo(expectedValue) > 0) {
            warning = "Raw value is higher than PSA 9 value. PSA 9 may underperform raw.";
        }
        GradingRecommendation recommendation = profitable
                ? GradingRecommendation.STRONG_GRADE
                : warning == null ? GradingRecommendation.HOLD_RAW : GradingRecommendation.DO_NOT_GRADE;
        return new ScenarioCalculation(type, grade, expectedValue, totalCost, expectedProfit, roi, recommendation, warning);
    }

    private GradingRecommendation finalRecommendation(
            ScenarioCalculation conservative,
            ScenarioCalculation balanced,
            ScenarioCalculation aggressive,
            BigDecimal rawValue,
            BigDecimal psa8Value,
            BigDecimal psa9Value) {
        if (conservative.expectedValue().compareTo(BigDecimal.ZERO) == 0
                && balanced.expectedValue().compareTo(BigDecimal.ZERO) == 0
                && aggressive.expectedValue().compareTo(BigDecimal.ZERO) == 0) {
            return GradingRecommendation.INSUFFICIENT_DATA;
        }
        if (conservative.profitable() || balanced.profitable()) {
            return GradingRecommendation.STRONG_GRADE;
        }
        if (aggressive.profitable()) {
            return GradingRecommendation.GRADE_ONLY_IF_CONFIDENT_PSA10;
        }
        if (rawValue.compareTo(psa8Value) > 0 || rawValue.compareTo(psa9Value) > 0) {
            return GradingRecommendation.DO_NOT_GRADE;
        }
        return GradingRecommendation.HOLD_RAW;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ScenarioCalculation(
            GradingScenarioType type,
            PsaGrade grade,
            BigDecimal expectedValue,
            BigDecimal totalCost,
            BigDecimal expectedProfit,
            BigDecimal roi,
            GradingRecommendation recommendation,
            String warning) {

        boolean profitable() {
            return expectedProfit.compareTo(DEFAULT_MINIMUM_PROFIT_THRESHOLD_SGD) >= 0;
        }

        ScenarioCalculation withWarning(String warning) {
            return new ScenarioCalculation(
                    type,
                    grade,
                    expectedValue,
                    totalCost,
                    expectedProfit,
                    roi,
                    GradingRecommendation.GRADE_ONLY_IF_CONFIDENT_PSA10,
                    warning);
        }

        GradingScenario toEntity(GradingAnalysis analysis) {
            return new GradingScenario(
                    analysis,
                    type,
                    grade,
                    expectedValue,
                    totalCost,
                    expectedProfit,
                    roi,
                    recommendation,
                    warning);
        }
    }
}
