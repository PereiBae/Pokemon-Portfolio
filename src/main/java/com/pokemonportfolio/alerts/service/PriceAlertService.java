package com.pokemonportfolio.alerts.service;

import com.pokemonportfolio.alerts.entity.Alert;
import com.pokemonportfolio.alerts.repository.AlertRepository;
import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceAlertService {

    private static final BigDecimal LOW_PURCHASE_BOUNDARY = new BigDecimal("100.00");
    private static final BigDecimal LOW_PURCHASE_GAIN_THRESHOLD = new BigDecimal("10.00");
    private static final BigDecimal HIGH_PURCHASE_GAIN_THRESHOLD = new BigDecimal("25.00");

    private final OwnedItemService ownedItemService;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final AlertRepository alertRepository;

    public PriceAlertService(
            OwnedItemService ownedItemService,
            PriceSnapshotRepository priceSnapshotRepository,
            AlertRepository alertRepository) {
        this.ownedItemService = ownedItemService;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional
    public List<Alert> checkAlerts(AppUser owner) {
        List<Alert> createdAlerts = new ArrayList<>();
        for (OwnedItem ownedItem : ownedItemService.listActiveItems(owner)) {
            priceSnapshotRepository
                    .findTopByCardIdOrderByCalculatedAtDescIdDesc(ownedItem.getCard().getId())
                    .ifPresent(snapshot -> createIfThresholdMet(owner, ownedItem, snapshot, createdAlerts));
        }
        return createdAlerts;
    }

    private void createIfThresholdMet(
            AppUser owner,
            OwnedItem ownedItem,
            PriceSnapshot snapshot,
            List<Alert> createdAlerts) {
        if (alertRepository.existsByOwnedItemIdAndPriceSnapshotId(ownedItem.getId(), snapshot.getId())) {
            return;
        }

        BigDecimal purchasePrice = MoneyCalculationSupport.money(ownedItem.getPurchasePriceSgd());
        BigDecimal marketValue = MoneyCalculationSupport.money(snapshot.getMarketPriceSgd());
        BigDecimal gainAmount = MoneyCalculationSupport.money(marketValue.subtract(purchasePrice));
        if (gainAmount.compareTo(thresholdFor(purchasePrice)) < 0) {
            return;
        }

        Alert alert = new Alert(
                owner,
                ownedItem,
                snapshot,
                displayName(ownedItem),
                purchasePrice,
                marketValue,
                gainAmount,
                MoneyCalculationSupport.percent(gainAmount, purchasePrice),
                snapshot.getConfidenceRating(),
                OffsetDateTime.now());
        createdAlerts.add(alertRepository.save(alert));
    }

    private BigDecimal thresholdFor(BigDecimal purchasePrice) {
        if (purchasePrice.compareTo(LOW_PURCHASE_BOUNDARY) < 0) {
            return LOW_PURCHASE_GAIN_THRESHOLD;
        }
        return HIGH_PURCHASE_GAIN_THRESHOLD;
    }

    private String displayName(OwnedItem ownedItem) {
        return ownedItem.getCard().getName()
                + " #"
                + ownedItem.getCard().getCardNumber()
                + " - "
                + ownedItem.getCard().getPokemonSet().getName()
                + " ("
                + ownedItem.getOwnedVariant().getLabel()
                + ")";
    }
}
