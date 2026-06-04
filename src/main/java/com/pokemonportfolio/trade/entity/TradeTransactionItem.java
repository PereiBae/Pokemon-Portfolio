package com.pokemonportfolio.trade.entity;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.config.domain.AssetType;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.SealedProductCondition;
import com.pokemonportfolio.config.domain.TradeSideType;
import com.pokemonportfolio.config.entity.AuditableEntity;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.entity.OwnedItemDisposal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "trade_transaction_item")
public class TradeTransactionItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_transaction_id", nullable = false)
    private TradeTransaction tradeTransaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_transaction_side_id", nullable = false)
    private TradeTransactionSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "side_type", nullable = false)
    private TradeSideType sideType;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType = AssetType.CARD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outgoing_owned_item_id")
    private OwnedItem outgoingOwnedItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sealed_product_id")
    private SealedProduct sealedProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incoming_owned_item_id")
    private OwnedItem incomingOwnedItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposal_id")
    private OwnedItemDisposal disposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "incoming_variant")
    private CardVariant incomingVariant;

    @Enumerated(EnumType.STRING)
    @Column(name = "incoming_condition")
    private CardCondition incomingCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "incoming_sealed_condition")
    private SealedProductCondition incomingSealedCondition;

    @Column(name = "agreed_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal agreedValueSgd = BigDecimal.ZERO;

    @Column(name = "override_value_sgd", precision = 19, scale = 2)
    private BigDecimal overrideValueSgd;

    @Column(name = "market_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal marketValueSgd = BigDecimal.ZERO;

    @Column(name = "adjusted_value_sgd", nullable = false, precision = 19, scale = 2)
    private BigDecimal adjustedValueSgd = BigDecimal.ZERO;

    @Column(name = "allocated_cost_basis_sgd", precision = 19, scale = 2)
    private BigDecimal allocatedCostBasisSgd;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_rating", nullable = false)
    private ConfidenceRating confidenceRating = ConfidenceRating.LOW;

    @Column(length = 1000)
    private String notes;

    protected TradeTransactionItem() {
    }

    public static TradeTransactionItem outgoing(
            TradeTransaction tradeTransaction,
            TradeTransactionSide side,
            OwnedItem outgoingOwnedItem,
            BigDecimal overrideValueSgd,
            String notes) {
        TradeTransactionItem item = new TradeTransactionItem();
        item.tradeTransaction = tradeTransaction;
        item.side = side;
        item.sideType = TradeSideType.OUTGOING;
        item.assetType = outgoingOwnedItem.getAssetType();
        item.outgoingOwnedItem = outgoingOwnedItem;
        item.card = outgoingOwnedItem.getCard();
        item.sealedProduct = outgoingOwnedItem.getSealedProduct();
        item.overrideValueSgd = overrideValueSgd;
        item.agreedValueSgd = overrideValueSgd == null ? BigDecimal.ZERO : overrideValueSgd;
        item.notes = notes;
        return item;
    }

    public static TradeTransactionItem incoming(
            TradeTransaction tradeTransaction,
            TradeTransactionSide side,
            Card card,
            CardVariant incomingVariant,
            CardCondition incomingCondition,
            BigDecimal overrideValueSgd,
            String notes) {
        TradeTransactionItem item = new TradeTransactionItem();
        item.tradeTransaction = tradeTransaction;
        item.side = side;
        item.sideType = TradeSideType.INCOMING;
        item.assetType = AssetType.CARD;
        item.card = card;
        item.incomingVariant = incomingVariant;
        item.incomingCondition = incomingCondition;
        item.overrideValueSgd = overrideValueSgd;
        item.agreedValueSgd = overrideValueSgd == null ? BigDecimal.ZERO : overrideValueSgd;
        item.notes = notes;
        return item;
    }

    public static TradeTransactionItem incomingSealedProduct(
            TradeTransaction tradeTransaction,
            TradeTransactionSide side,
            SealedProduct sealedProduct,
            SealedProductCondition incomingSealedCondition,
            BigDecimal overrideValueSgd,
            String notes) {
        TradeTransactionItem item = new TradeTransactionItem();
        item.tradeTransaction = tradeTransaction;
        item.side = side;
        item.sideType = TradeSideType.INCOMING;
        item.assetType = AssetType.SEALED_PRODUCT;
        item.sealedProduct = sealedProduct;
        item.incomingSealedCondition = incomingSealedCondition;
        item.overrideValueSgd = overrideValueSgd;
        item.agreedValueSgd = overrideValueSgd == null ? BigDecimal.ZERO : overrideValueSgd;
        item.notes = notes;
        return item;
    }

    public Long getId() {
        return id;
    }

    public TradeTransaction getTradeTransaction() {
        return tradeTransaction;
    }

    public TradeTransactionSide getSide() {
        return side;
    }

    public TradeSideType getSideType() {
        return sideType;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public OwnedItem getOutgoingOwnedItem() {
        return outgoingOwnedItem;
    }

    public Card getCard() {
        return card;
    }

    public SealedProduct getSealedProduct() {
        return sealedProduct;
    }

    public OwnedItem getIncomingOwnedItem() {
        return incomingOwnedItem;
    }

    public OwnedItemDisposal getDisposal() {
        return disposal;
    }

    public CardVariant getIncomingVariant() {
        return incomingVariant;
    }

    public CardCondition getIncomingCondition() {
        return incomingCondition;
    }

    public SealedProductCondition getIncomingSealedCondition() {
        return incomingSealedCondition;
    }

    public BigDecimal getAgreedValueSgd() {
        return agreedValueSgd;
    }

    public BigDecimal getOverrideValueSgd() {
        return overrideValueSgd;
    }

    public BigDecimal getMarketValueSgd() {
        return marketValueSgd;
    }

    public BigDecimal getAdjustedValueSgd() {
        return adjustedValueSgd;
    }

    public BigDecimal getAllocatedCostBasisSgd() {
        return allocatedCostBasisSgd;
    }

    public ConfidenceRating getConfidenceRating() {
        return confidenceRating;
    }

    public String getNotes() {
        return notes;
    }

    public void updateCalculatedValues(
            BigDecimal marketValueSgd,
            BigDecimal baseValueSgd,
            BigDecimal adjustedValueSgd,
            ConfidenceRating confidenceRating) {
        this.marketValueSgd = marketValueSgd;
        this.agreedValueSgd = baseValueSgd;
        this.adjustedValueSgd = adjustedValueSgd;
        this.confidenceRating = confidenceRating;
    }

    public void linkDisposal(OwnedItemDisposal disposal) {
        this.disposal = disposal;
    }

    public void linkIncomingOwnedItem(OwnedItem incomingOwnedItem, BigDecimal allocatedCostBasisSgd) {
        this.incomingOwnedItem = incomingOwnedItem;
        this.allocatedCostBasisSgd = allocatedCostBasisSgd;
    }
}
