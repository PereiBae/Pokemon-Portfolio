package com.pokemonportfolio.trade.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.SealedProductService;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.SealedProductCondition;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.config.domain.TradeSideType;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import com.pokemonportfolio.trade.entity.TradeTransaction;
import com.pokemonportfolio.trade.entity.TradeTransactionItem;
import com.pokemonportfolio.trade.entity.TradeTransactionSide;
import com.pokemonportfolio.trade.repository.TradeTransactionItemRepository;
import com.pokemonportfolio.trade.repository.TradeTransactionRepository;
import com.pokemonportfolio.trade.repository.TradeTransactionSideRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeTransactionService {

    private final TradeTransactionRepository tradeTransactionRepository;
    private final TradeTransactionSideRepository tradeTransactionSideRepository;
    private final TradeTransactionItemRepository tradeTransactionItemRepository;
    private final OwnedItemService ownedItemService;
    private final CardService cardService;
    private final SealedProductService sealedProductService;
    private final PriceSnapshotRepository priceSnapshotRepository;

    public TradeTransactionService(
            TradeTransactionRepository tradeTransactionRepository,
            TradeTransactionSideRepository tradeTransactionSideRepository,
            TradeTransactionItemRepository tradeTransactionItemRepository,
            OwnedItemService ownedItemService,
            CardService cardService,
            SealedProductService sealedProductService,
            PriceSnapshotRepository priceSnapshotRepository) {
        this.tradeTransactionRepository = tradeTransactionRepository;
        this.tradeTransactionSideRepository = tradeTransactionSideRepository;
        this.tradeTransactionItemRepository = tradeTransactionItemRepository;
        this.ownedItemService = ownedItemService;
        this.cardService = cardService;
        this.sealedProductService = sealedProductService;
        this.priceSnapshotRepository = priceSnapshotRepository;
    }

    @Transactional
    public TradeTransaction createDraft(AppUser owner, TradeCreateForm form) {
        TradeTransaction tradeTransaction = new TradeTransaction(
                owner,
                requireText(form.getName(), "Trade name is required"),
                blankToNull(form.getNotes()));
        TradeTransaction saved = tradeTransactionRepository.save(tradeTransaction);
        tradeTransactionSideRepository.save(new TradeTransactionSide(
                saved,
                TradeSideType.OUTGOING,
                requirePositivePercentage(form.getOutgoingTradePercentage())));
        tradeTransactionSideRepository.save(new TradeTransactionSide(
                saved,
                TradeSideType.INCOMING,
                requirePositivePercentage(form.getIncomingTradePercentage())));
        return saved;
    }

    @Transactional
    public void updatePercentages(AppUser owner, Long tradeTransactionId, TradePercentageForm form) {
        TradeTransaction tradeTransaction = requireEditableTransaction(owner, tradeTransactionId);
        side(tradeTransaction, TradeSideType.OUTGOING)
                .updateTradePercentage(requirePositivePercentage(form.getOutgoingTradePercentage()));
        side(tradeTransaction, TradeSideType.INCOMING)
                .updateTradePercentage(requirePositivePercentage(form.getIncomingTradePercentage()));
    }

    @Transactional
    public TradeTransactionItem addOutgoingItem(AppUser owner, Long tradeTransactionId, TradeOutgoingItemForm form) {
        TradeTransaction tradeTransaction = requireEditableTransaction(owner, tradeTransactionId);
        if (form.getOwnedItemId() == null) {
            throw new IllegalArgumentException("Please select an outgoing portfolio item.");
        }
        OwnedItem ownedItem = ownedItemService.requireActiveItemForOwner(owner, form.getOwnedItemId());
        if (ownedItem.getStatus() != OwnedItemStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active portfolio items can be traded");
        }
        if (tradeTransactionItemRepository.existsByTradeTransactionAndOutgoingOwnedItemId(
                tradeTransaction,
                ownedItem.getId())) {
            throw new IllegalArgumentException("This portfolio item is already on the outgoing side");
        }
        TradeTransactionSide side = side(tradeTransaction, TradeSideType.OUTGOING);
        TradeValueSelection valueSelection = resolveTradeValue(
                ownedItem,
                form.getOverrideValueSgd(),
                "No market value found. Add a manual price entry or provide an override value.");
        TradeTransactionItem item = TradeTransactionItem.outgoing(
                tradeTransaction,
                side,
                ownedItem,
                valueSelection.overrideValueSgd(),
                blankToNull(form.getNotes()));
        item.updateCalculatedValues(
                valueSelection.marketValueSgd(),
                valueSelection.baseValueSgd(),
                adjustedValue(valueSelection.baseValueSgd(), side.getTradePercentage()),
                valueSelection.confidenceRating());
        return tradeTransactionItemRepository.save(item);
    }

    @Transactional
    public TradeTransactionItem addIncomingItem(AppUser owner, Long tradeTransactionId, TradeIncomingItemForm form) {
        TradeTransaction tradeTransaction = requireEditableTransaction(owner, tradeTransactionId);
        boolean hasCard = form.getCardId() != null;
        boolean hasSealedProduct = form.getSealedProductId() != null;
        if (!hasCard && !hasSealedProduct) {
            throw new IllegalArgumentException("Please select an incoming card or sealed product.");
        }
        if (hasCard && hasSealedProduct) {
            throw new IllegalArgumentException("Select either an incoming card or sealed product, not both.");
        }
        if (hasSealedProduct) {
            return addIncomingSealedProduct(tradeTransaction, form);
        }
        Card card = cardService.requireCard(form.getCardId());
        CardVariant selectedVariant = form.getVariant() == null ? card.getDefaultOwnedVariant() : form.getVariant();
        CardCondition selectedCondition = form.getCondition() == null
                ? CardCondition.RAW_NEAR_MINT
                : form.getCondition();
        TradeTransactionSide side = side(tradeTransaction, TradeSideType.INCOMING);
        TradeValueSelection valueSelection = resolveTradeValue(
                card,
                form.getOverrideValueSgd(),
                "No market value found for this incoming card. Enter an Override Value SGD to continue.");
        TradeTransactionItem item = TradeTransactionItem.incoming(
                tradeTransaction,
                side,
                card,
                selectedVariant,
                selectedCondition,
                valueSelection.overrideValueSgd(),
                blankToNull(form.getNotes()));
        item.updateCalculatedValues(
                valueSelection.marketValueSgd(),
                valueSelection.baseValueSgd(),
                adjustedValue(valueSelection.baseValueSgd(), side.getTradePercentage()),
                valueSelection.confidenceRating());
        return tradeTransactionItemRepository.save(item);
    }

    private TradeTransactionItem addIncomingSealedProduct(
            TradeTransaction tradeTransaction,
            TradeIncomingItemForm form) {
        SealedProduct sealedProduct = sealedProductService.requireSealedProduct(form.getSealedProductId());
        SealedProductCondition selectedCondition = form.getSealedCondition() == null
                ? SealedProductCondition.SEALED
                : form.getSealedCondition();
        TradeTransactionSide side = side(tradeTransaction, TradeSideType.INCOMING);
        TradeValueSelection valueSelection = resolveTradeValue(
                sealedProduct,
                form.getOverrideValueSgd(),
                "No market value found for this incoming sealed product. Enter an Override Value SGD to continue.");
        TradeTransactionItem item = TradeTransactionItem.incomingSealedProduct(
                tradeTransaction,
                side,
                sealedProduct,
                selectedCondition,
                valueSelection.overrideValueSgd(),
                blankToNull(form.getNotes()));
        item.updateCalculatedValues(
                valueSelection.marketValueSgd(),
                valueSelection.baseValueSgd(),
                adjustedValue(valueSelection.baseValueSgd(), side.getTradePercentage()),
                valueSelection.confidenceRating());
        return tradeTransactionItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<TradeTransactionListView> list(AppUser owner) {
        return tradeTransactionRepository.findByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(this::toListView)
                .toList();
    }

    @Transactional(readOnly = true)
    public TradeTransactionView detailView(AppUser owner, Long tradeTransactionId) {
        TradeTransaction tradeTransaction = requireTransaction(owner, tradeTransactionId);
        Map<TradeSideType, TradeTransactionSide> sides = tradeTransactionSideRepository
                .findByTradeTransaction(tradeTransaction)
                .stream()
                .collect(Collectors.toMap(TradeTransactionSide::getSideType, Function.identity()));
        List<TradeTransactionItem> items = tradeTransactionItemRepository
                .findByTradeTransactionOrderByCreatedAtAscIdAsc(tradeTransaction);
        return toDetailView(tradeTransaction, sides, items);
    }

    @Transactional(readOnly = true)
    public TradeTransaction requireTransaction(AppUser owner, Long tradeTransactionId) {
        return tradeTransactionRepository.findByIdAndOwner(tradeTransactionId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Trade transaction not found"));
    }

    private TradeTransaction requireEditableTransaction(AppUser owner, Long tradeTransactionId) {
        TradeTransaction tradeTransaction = requireTransaction(owner, tradeTransactionId);
        if (tradeTransaction.isExecuted()) {
            throw new IllegalArgumentException("Executed trades cannot be changed");
        }
        if (tradeTransaction.isCancelled()) {
            throw new IllegalArgumentException("Cancelled trades cannot be changed");
        }
        return tradeTransaction;
    }

    private TradeTransactionSide side(TradeTransaction tradeTransaction, TradeSideType sideType) {
        return tradeTransactionSideRepository.findByTradeTransactionAndSideType(tradeTransaction, sideType)
                .orElseThrow(() -> new IllegalStateException(sideType.getLabel() + " trade side is missing"));
    }

    private TradeTransactionListView toListView(TradeTransaction tradeTransaction) {
        return new TradeTransactionListView(
                tradeTransaction.getId(),
                tradeTransaction.getName(),
                tradeTransaction.getStatus(),
                tradeTransaction.getStatus().getLabel(),
                tradeTransaction.getFairnessResult(),
                tradeTransaction.getFairnessResult().getLabel(),
                tradeTransaction.getConfidenceRating(),
                tradeTransaction.getTotalOutgoingAdjustedValueSgd(),
                tradeTransaction.getTotalIncomingAdjustedValueSgd(),
                tradeTransaction.getNetDifferenceSgd(),
                tradeTransaction.getTradeImbalanceSgd(),
                tradeTransaction.getCreatedAt(),
                tradeTransaction.getExecutedAt());
    }

    private TradeTransactionView toDetailView(
            TradeTransaction tradeTransaction,
            Map<TradeSideType, TradeTransactionSide> sides,
            List<TradeTransactionItem> items) {
        List<TradeItemView> outgoingItems = items.stream()
                .filter(item -> item.getSideType() == TradeSideType.OUTGOING)
                .map(this::toItemView)
                .toList();
        List<TradeItemView> incomingItems = items.stream()
                .filter(item -> item.getSideType() == TradeSideType.INCOMING)
                .map(this::toItemView)
                .toList();
        return new TradeTransactionView(
                tradeTransaction.getId(),
                tradeTransaction.getName(),
                tradeTransaction.getMode(),
                tradeTransaction.getMode().getLabel(),
                tradeTransaction.getStatus(),
                tradeTransaction.getStatus().getLabel(),
                sides.get(TradeSideType.OUTGOING).getTradePercentage(),
                sides.get(TradeSideType.INCOMING).getTradePercentage(),
                tradeTransaction.getTotalOutgoingMarketValueSgd(),
                tradeTransaction.getTotalIncomingMarketValueSgd(),
                tradeTransaction.getTotalOutgoingAgreedValueSgd(),
                tradeTransaction.getTotalIncomingAgreedValueSgd(),
                tradeTransaction.getTotalOutgoingAdjustedValueSgd(),
                tradeTransaction.getTotalIncomingAdjustedValueSgd(),
                tradeTransaction.getNetDifferenceSgd(),
                tradeTransaction.getTradeImbalanceSgd(),
                tradeTransaction.getFairnessResult(),
                tradeTransaction.getFairnessResult().getLabel(),
                tradeTransaction.getConfidenceRating(),
                tradeTransaction.getNotes(),
                tradeTransaction.getAnalysedAt(),
                tradeTransaction.getExecutedAt(),
                outgoingItems,
                incomingItems);
    }

    private TradeItemView toItemView(TradeTransactionItem item) {
        String itemDisplayName;
        String imageSmallUrl;
        String variantLabel;
        String conditionLabel;
        if (item.getSideType() == TradeSideType.OUTGOING) {
            OwnedItem ownedItem = item.getOutgoingOwnedItem();
            itemDisplayName = ownedItem.displayName();
            imageSmallUrl = ownedItem.imageSmallUrl();
            variantLabel = ownedItem.variantOrTypeLabel();
            conditionLabel = ownedItem.conditionLabel();
        } else {
            if (item.getCard() != null) {
                Card card = item.getCard();
                itemDisplayName = card.getName() + " #" + card.getCardNumber() + " - " + card.getPokemonSet().getName();
                imageSmallUrl = card.getExternalImageSmallUrl();
                variantLabel = item.getIncomingVariant().getLabel();
                conditionLabel = item.getIncomingCondition().getLabel();
            } else {
                SealedProduct sealedProduct = item.getSealedProduct();
                itemDisplayName = sealedProductDisplayName(sealedProduct);
                imageSmallUrl = sealedProduct.getImageUrl();
                variantLabel = sealedProduct.getProductType().getLabel();
                conditionLabel = item.getIncomingSealedCondition().getLabel();
            }
        }
        return new TradeItemView(
                item.getId(),
                item.getSideType(),
                itemDisplayName,
                imageSmallUrl,
                variantLabel,
                conditionLabel,
                item.getMarketValueSgd(),
                item.getOverrideValueSgd(),
                item.getAgreedValueSgd(),
                item.getAdjustedValueSgd(),
                item.getAllocatedCostBasisSgd(),
                item.getConfidenceRating(),
                item.getOutgoingOwnedItem() == null ? null : item.getOutgoingOwnedItem().getId(),
                item.getIncomingOwnedItem() == null ? null : item.getIncomingOwnedItem().getId(),
                item.getDisposal() == null ? null : item.getDisposal().getId(),
                item.getNotes());
    }

    private BigDecimal requirePositivePercentage(BigDecimal value) {
        BigDecimal percentage = value == null
                ? new BigDecimal("100.0000")
                : value.setScale(4, RoundingMode.HALF_UP);
        if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Trade percentage must be positive");
        }
        return percentage;
    }

    private TradeValueSelection resolveTradeValue(Card card, BigDecimal overrideValue, String missingValueMessage) {
        return resolveTradeValue(
                priceSnapshotRepository.findTopByCardIdOrderByCalculatedAtDescIdDesc(card.getId()),
                overrideValue,
                missingValueMessage);
    }

    private TradeValueSelection resolveTradeValue(
            SealedProduct sealedProduct,
            BigDecimal overrideValue,
            String missingValueMessage) {
        return resolveTradeValue(
                priceSnapshotRepository.findTopBySealedProductIdOrderByCalculatedAtDescIdDesc(sealedProduct.getId()),
                overrideValue,
                missingValueMessage);
    }

    private TradeValueSelection resolveTradeValue(OwnedItem ownedItem, BigDecimal overrideValue, String missingValueMessage) {
        Optional<PriceSnapshot> latestSnapshot = ownedItem.isSealedProduct()
                ? priceSnapshotRepository.findTopBySealedProductIdOrderByCalculatedAtDescIdDesc(
                        ownedItem.getSealedProduct().getId())
                : priceSnapshotRepository.findTopByCardIdOrderByCalculatedAtDescIdDesc(ownedItem.getCard().getId());
        return resolveTradeValue(latestSnapshot, overrideValue, missingValueMessage);
    }

    private TradeValueSelection resolveTradeValue(
            Optional<PriceSnapshot> latestSnapshot,
            BigDecimal overrideValue,
            String missingValueMessage) {
        BigDecimal overrideMoney = optionalNonNegativeMoney(overrideValue);
        PriceSnapshot snapshot = latestSnapshot.orElse(null);
        if (overrideMoney == null && snapshot == null) {
            throw new IllegalArgumentException(missingValueMessage);
        }
        BigDecimal marketValue = snapshot == null
                ? BigDecimal.ZERO.setScale(2)
                : MoneyCalculationSupport.money(snapshot.getMarketPriceSgd());
        ConfidenceRating confidenceRating = snapshot == null
                ? ConfidenceRating.LOW
                : snapshot.getConfidenceRating();
        BigDecimal baseValue = overrideMoney == null ? marketValue : overrideMoney;
        return new TradeValueSelection(overrideMoney, marketValue, baseValue, confidenceRating);
    }

    private BigDecimal optionalNonNegativeMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal money = MoneyCalculationSupport.money(value);
        if (money.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Override value must be non-negative");
        }
        return money;
    }

    private BigDecimal adjustedValue(BigDecimal baseValueSgd, BigDecimal tradePercentage) {
        return MoneyCalculationSupport.money(baseValueSgd
                .multiply(tradePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String sealedProductDisplayName(SealedProduct sealedProduct) {
        if (sealedProduct.getSetName() == null || sealedProduct.getSetName().isBlank()) {
            return sealedProduct.getName();
        }
        return sealedProduct.getName() + " - " + sealedProduct.getSetName();
    }

    private record TradeValueSelection(
            BigDecimal overrideValueSgd,
            BigDecimal marketValueSgd,
            BigDecimal baseValueSgd,
            ConfidenceRating confidenceRating) {
    }
}
