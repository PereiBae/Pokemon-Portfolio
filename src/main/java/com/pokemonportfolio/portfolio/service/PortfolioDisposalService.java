package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.alerts.service.AlertViewService;
import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.DisposalType;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.entity.OwnedItemDisposal;
import com.pokemonportfolio.portfolio.repository.OwnedItemDisposalRepository;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioDisposalService {

    private final OwnedItemService ownedItemService;
    private final OwnedItemDisposalRepository disposalRepository;
    private final AlertViewService alertViewService;

    public PortfolioDisposalService(
            OwnedItemService ownedItemService,
            OwnedItemDisposalRepository disposalRepository,
            AlertViewService alertViewService) {
        this.ownedItemService = ownedItemService;
        this.disposalRepository = disposalRepository;
        this.alertViewService = alertViewService;
    }

    @Transactional
    public OwnedItemDisposal sellItem(AppUser owner, Long ownedItemId, OwnedItemDisposalForm form) {
        OwnedItem item = ownedItemService.requireActiveItemForOwner(owner, ownedItemId);
        BigDecimal salePrice = requireNonNegative(form.getSalePriceSgd(), "Sale price is required");
        BigDecimal fees = MoneyCalculationSupport.money(form.getFeesSgd());
        BigDecimal netProceeds = MoneyCalculationSupport.money(salePrice.subtract(fees));
        BigDecimal purchasePrice = MoneyCalculationSupport.money(item.getPurchasePriceSgd());
        BigDecimal realizedGain = MoneyCalculationSupport.money(netProceeds.subtract(purchasePrice));
        OwnedItemDisposal disposal = new OwnedItemDisposal(
                owner,
                item,
                DisposalType.SOLD,
                form.getDisposalDate(),
                salePrice,
                fees,
                netProceeds,
                purchasePrice,
                realizedGain,
                MoneyCalculationSupport.percent(realizedGain, purchasePrice),
                blankToNull(form.getNotes()));
        item.markSold(OffsetDateTime.now());
        OwnedItemDisposal saved = disposalRepository.save(disposal);
        alertViewService.dismissActiveAlertsForOwnedItem(owner, ownedItemId);
        return saved;
    }

    @Transactional
    public OwnedItemDisposal tradeAwayItem(AppUser owner, Long ownedItemId, OwnedItemDisposalForm form) {
        OwnedItem item = ownedItemService.requireActiveItemForOwner(owner, ownedItemId);
        BigDecimal tradeValue = requireNonNegative(form.getTradeValueSgd(), "Trade value received is required");
        BigDecimal purchasePrice = MoneyCalculationSupport.money(item.getPurchasePriceSgd());
        BigDecimal realizedGain = MoneyCalculationSupport.money(tradeValue.subtract(purchasePrice));
        OwnedItemDisposal disposal = new OwnedItemDisposal(
                owner,
                item,
                DisposalType.TRADED,
                form.getDisposalDate(),
                tradeValue,
                BigDecimal.ZERO.setScale(2),
                tradeValue,
                purchasePrice,
                realizedGain,
                MoneyCalculationSupport.percent(realizedGain, purchasePrice),
                blankToNull(form.getNotes()));
        item.markTraded(OffsetDateTime.now());
        OwnedItemDisposal saved = disposalRepository.save(disposal);
        alertViewService.dismissActiveAlertsForOwnedItem(owner, ownedItemId);
        return saved;
    }

    @Transactional
    public OwnedItemDisposal deleteMistake(AppUser owner, Long ownedItemId, OwnedItemDisposalForm form) {
        OwnedItem item = ownedItemService.requireActiveItemForOwner(owner, ownedItemId);
        BigDecimal purchasePrice = MoneyCalculationSupport.money(item.getPurchasePriceSgd());
        OwnedItemDisposal disposal = new OwnedItemDisposal(
                owner,
                item,
                DisposalType.DELETED,
                form.getDisposalDate(),
                null,
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                purchasePrice,
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(4),
                blankToNull(form.getNotes()));
        item.markDeleted(OffsetDateTime.now());
        OwnedItemDisposal saved = disposalRepository.save(disposal);
        alertViewService.dismissActiveAlertsForOwnedItem(owner, ownedItemId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PortfolioDisposalView> listDisposalHistory(AppUser owner) {
        return disposalRepository.findByOwnerOrderByDisposalDateDescIdDesc(owner).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PortfolioDisposalSummary realizedSummary(AppUser owner) {
        List<OwnedItemDisposal> disposals = disposalRepository.findByOwnerAndDisposalTypeIn(
                owner,
                List.of(DisposalType.SOLD, DisposalType.TRADED));
        BigDecimal realizedGain = disposals.stream()
                .map(OwnedItemDisposal::getRealizedGainLossSgd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realizedCostBasis = disposals.stream()
                .map(OwnedItemDisposal::getOriginalPurchasePriceSgd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PortfolioDisposalSummary(
                MoneyCalculationSupport.money(realizedGain),
                MoneyCalculationSupport.percent(realizedGain, realizedCostBasis),
                MoneyCalculationSupport.money(realizedCostBasis));
    }

    private PortfolioDisposalView toView(OwnedItemDisposal disposal) {
        return new PortfolioDisposalView(
                disposal.getId(),
                disposal.getOwnedItem().getId(),
                displayName(disposal.getOwnedItem()),
                disposal.getDisposalType(),
                disposal.getDisposalType().getLabel(),
                disposal.getDisposalDate(),
                disposal.getOriginalPurchasePriceSgd(),
                disposal.getProceedsValueSgd(),
                disposal.getFeesSgd(),
                disposal.getNetProceedsSgd(),
                disposal.getRealizedGainLossSgd(),
                disposal.getRealizedGainLossPercent(),
                disposal.getNotes());
    }

    private BigDecimal requireNonNegative(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        BigDecimal money = MoneyCalculationSupport.money(value);
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
        return money;
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
