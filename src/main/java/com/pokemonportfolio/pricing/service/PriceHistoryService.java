package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.HistoryRange;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceHistoryService {

    private static final BigDecimal CHART_LEFT = new BigDecimal("24");
    private static final BigDecimal CHART_TOP = new BigDecimal("16");
    private static final BigDecimal CHART_WIDTH = new BigDecimal("952");
    private static final BigDecimal CHART_HEIGHT = new BigDecimal("220");
    private static final DateTimeFormatter POINT_LABEL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OwnedItemService ownedItemService;
    private final PriceSnapshotRepository priceSnapshotRepository;

    public PriceHistoryService(OwnedItemService ownedItemService, PriceSnapshotRepository priceSnapshotRepository) {
        this.ownedItemService = ownedItemService;
        this.priceSnapshotRepository = priceSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public OwnedItemPriceHistoryPageView historyFor(AppUser owner, Long ownedItemId, String rangeCode) {
        OwnedItem item = ownedItemService.requireItemForOwner(owner, ownedItemId);
        HistoryRange selectedRange = HistoryRange.fromCode(rangeCode);
        List<PriceSnapshot> snapshots = historySnapshots(item, selectedRange);
        Optional<PriceSnapshot> latest = latestSnapshot(item);
        BigDecimal latestMarketValue = latest
                .map(PriceSnapshot::getMarketPriceSgd)
                .map(MoneyCalculationSupport::money)
                .orElse(null);
        BigDecimal gainLoss = latestMarketValue == null
                ? null
                : MoneyCalculationSupport.money(latestMarketValue.subtract(item.getPurchasePriceSgd()));
        return new OwnedItemPriceHistoryPageView(
                item.getId(),
                item.getAssetType().getLabel(),
                item.assetName(),
                item.isSealedProduct() ? item.variantOrTypeLabel() : item.setName(),
                item.variantOrTypeLabel(),
                item.conditionLabel(),
                imageUrl(item),
                item.getPurchasePriceSgd(),
                latestMarketValue,
                gainLoss,
                gainLoss == null ? null : MoneyCalculationSupport.percent(gainLoss, item.getPurchasePriceSgd()),
                latest.map(PriceSnapshot::getConfidenceRating).orElse(null),
                latest.map(PriceSnapshot::getProviderName).orElse(""),
                latest.map(PriceSnapshot::getSourceMarket).orElse(""),
                latest.map(PriceSnapshot::getSourceCurrency).orElse(""),
                selectedRange,
                HistoryRange.options(),
                snapshots.stream().map(PriceSnapshotHistoryView::from).toList(),
                chartFor(snapshots));
    }

    private List<PriceSnapshot> historySnapshots(OwnedItem item, HistoryRange selectedRange) {
        Optional<OffsetDateTime> since = selectedRange.since(OffsetDateTime.now());
        if (item.isSealedProduct()) {
            return since
                    .map(value -> priceSnapshotRepository
                            .findBySealedProductIdAndCalculatedAtGreaterThanEqualOrderByCalculatedAtAscIdAsc(
                                    item.getSealedProduct().getId(),
                                    value))
                    .orElseGet(() -> priceSnapshotRepository
                            .findBySealedProductIdOrderByCalculatedAtAscIdAsc(item.getSealedProduct().getId()));
        }
        return since
                .map(value -> priceSnapshotRepository.findCardHistoryForOwnedVariantSince(
                        item.getCard().getId(),
                        item.getOwnedVariant(),
                        value))
                .orElseGet(() -> priceSnapshotRepository.findCardHistoryForOwnedVariant(
                        item.getCard().getId(),
                        item.getOwnedVariant()));
    }

    private Optional<PriceSnapshot> latestSnapshot(OwnedItem item) {
        if (item.isSealedProduct()) {
            return priceSnapshotRepository
                    .findTopBySealedProductIdOrderByCalculatedAtDescIdDesc(item.getSealedProduct().getId());
        }
        return priceSnapshotRepository
                .findTopByCardIdAndCardVariantOrderByCalculatedAtDescIdDesc(
                        item.getCard().getId(),
                        item.getOwnedVariant())
                .or(() -> priceSnapshotRepository
                        .findTopByCardIdAndCardVariantIsNullOrderByCalculatedAtDescIdDesc(
                                item.getCard().getId()));
    }

    private String imageUrl(OwnedItem item) {
        if (item.isSealedProduct()) {
            return item.getSealedProduct().getImageUrl();
        }
        String large = item.getCard().getExternalImageLargeUrl();
        if (large != null && !large.isBlank()) {
            return large;
        }
        return item.getCard().getExternalImageSmallUrl();
    }

    private PriceHistoryChartView chartFor(List<PriceSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return new PriceHistoryChartView(
                    List.of(),
                    "",
                    false,
                    "Not enough price history yet. Refresh prices or add manual price snapshots over time to build this chart.");
        }
        BigDecimal min = snapshots.stream()
                .map(PriceSnapshot::getMarketPriceSgd)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal max = snapshots.stream()
                .map(PriceSnapshot::getMarketPriceSgd)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal range = max.subtract(min);
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            range = BigDecimal.ONE;
        }
        int lastIndex = snapshots.size() - 1;
        BigDecimal divisor = new BigDecimal(lastIndex);
        BigDecimal minValue = min;
        BigDecimal valueRange = range;
        List<PriceHistoryChartPointView> points = java.util.stream.IntStream.range(0, snapshots.size())
                .mapToObj(index -> pointFor(snapshots.get(index), index, divisor, minValue, valueRange))
                .toList();
        String polyline = points.stream()
                .map(PriceHistoryChartPointView::coordinate)
                .collect(java.util.stream.Collectors.joining(" "));
        return new PriceHistoryChartView(points, polyline, true, "");
    }

    private PriceHistoryChartPointView pointFor(
            PriceSnapshot snapshot,
            int index,
            BigDecimal indexDivisor,
            BigDecimal minValue,
            BigDecimal valueRange) {
        BigDecimal x = CHART_LEFT.add(CHART_WIDTH.multiply(new BigDecimal(index))
                .divide(indexDivisor, 4, RoundingMode.HALF_UP));
        BigDecimal normalized = snapshot.getMarketPriceSgd().subtract(minValue)
                .divide(valueRange, 4, RoundingMode.HALF_UP);
        BigDecimal y = CHART_TOP.add(CHART_HEIGHT.subtract(CHART_HEIGHT.multiply(normalized)));
        String coordinate = formatCoordinate(x) + "," + formatCoordinate(y);
        return new PriceHistoryChartPointView(
                snapshot.getCalculatedAt(),
                MoneyCalculationSupport.money(snapshot.getMarketPriceSgd()),
                snapshot.getCalculatedAt().format(POINT_LABEL_FORMAT),
                coordinate);
    }

    private String formatCoordinate(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
