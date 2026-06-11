package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.HistoryRange;
import com.pokemonportfolio.portfolio.entity.PortfolioValuationSnapshot;
import com.pokemonportfolio.portfolio.repository.PortfolioValuationSnapshotRepository;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioHistoryService {

    private static final BigDecimal CHART_LEFT = new BigDecimal("24");
    private static final BigDecimal CHART_TOP = new BigDecimal("16");
    private static final BigDecimal CHART_WIDTH = new BigDecimal("952");
    private static final BigDecimal CHART_HEIGHT = new BigDecimal("220");
    private static final DateTimeFormatter POINT_LABEL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PortfolioValuationSnapshotRepository snapshotRepository;
    private final PortfolioValuationService valuationService;

    public PortfolioHistoryService(
            PortfolioValuationSnapshotRepository snapshotRepository,
            PortfolioValuationService valuationService) {
        this.snapshotRepository = snapshotRepository;
        this.valuationService = valuationService;
    }

    @Transactional(readOnly = true)
    public PortfolioHistoryPageView historyFor(AppUser owner, String rangeCode) {
        HistoryRange selectedRange = HistoryRange.fromCode(rangeCode);
        List<PortfolioValuationSnapshot> snapshots = selectedRange.since(OffsetDateTime.now())
                .map(since -> snapshotRepository.findByOwnerAndCalculatedAtGreaterThanEqualOrderByCalculatedAtAscIdAsc(owner, since))
                .orElseGet(() -> snapshotRepository.findByOwnerOrderByCalculatedAtAscIdAsc(owner));
        List<PortfolioHistorySnapshotView> snapshotViews = snapshots.stream()
                .map(PortfolioHistorySnapshotView::from)
                .toList();
        return new PortfolioHistoryPageView(
                selectedRange,
                HistoryRange.options(),
                snapshotViews,
                chartFor(snapshotViews),
                valuationService.calculateCurrentValue(owner));
    }

    private PortfolioHistoryChartView chartFor(List<PortfolioHistorySnapshotView> snapshots) {
        if (snapshots.size() < 2) {
            return new PortfolioHistoryChartView(
                    List.of(),
                    "",
                    false,
                    "Not enough portfolio history yet. Refresh prices and valuation snapshots over time to build this chart.");
        }

        BigDecimal min = snapshots.stream()
                .map(PortfolioHistorySnapshotView::totalValueSgd)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal max = snapshots.stream()
                .map(PortfolioHistorySnapshotView::totalValueSgd)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal range = max.subtract(min);
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            range = BigDecimal.ONE;
        }

        int lastIndex = snapshots.size() - 1;
        BigDecimal divisor = new BigDecimal(lastIndex);
        BigDecimal valueRange = range;
        BigDecimal minValue = min;
        List<PortfolioHistoryChartPointView> points = java.util.stream.IntStream.range(0, snapshots.size())
                .mapToObj(index -> pointFor(snapshots.get(index), index, divisor, minValue, valueRange))
                .toList();
        String polyline = points.stream()
                .map(PortfolioHistoryChartPointView::coordinate)
                .collect(java.util.stream.Collectors.joining(" "));
        return new PortfolioHistoryChartView(points, polyline, true, "");
    }

    private PortfolioHistoryChartPointView pointFor(
            PortfolioHistorySnapshotView snapshot,
            int index,
            BigDecimal indexDivisor,
            BigDecimal minValue,
            BigDecimal valueRange) {
        BigDecimal x = CHART_LEFT.add(CHART_WIDTH.multiply(new BigDecimal(index))
                .divide(indexDivisor, 4, RoundingMode.HALF_UP));
        BigDecimal normalized = snapshot.totalValueSgd().subtract(minValue)
                .divide(valueRange, 4, RoundingMode.HALF_UP);
        BigDecimal y = CHART_TOP.add(CHART_HEIGHT.subtract(CHART_HEIGHT.multiply(normalized)));
        String coordinate = formatCoordinate(x) + "," + formatCoordinate(y);
        return new PortfolioHistoryChartPointView(
                snapshot.calculatedAt(),
                MoneyCalculationSupport.money(snapshot.totalValueSgd()),
                snapshot.calculatedAt().format(POINT_LABEL_FORMAT),
                coordinate);
    }

    private String formatCoordinate(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
