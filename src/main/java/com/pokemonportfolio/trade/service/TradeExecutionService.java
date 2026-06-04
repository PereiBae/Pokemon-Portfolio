package com.pokemonportfolio.trade.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.config.domain.TradeSideType;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.entity.OwnedItemDisposal;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioDisposalService;
import com.pokemonportfolio.trade.entity.TradeTransaction;
import com.pokemonportfolio.trade.entity.TradeTransactionItem;
import com.pokemonportfolio.trade.repository.TradeTransactionItemRepository;
import com.pokemonportfolio.trade.repository.TradeTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeExecutionService {

    private final TradeTransactionRepository tradeTransactionRepository;
    private final TradeTransactionItemRepository tradeTransactionItemRepository;
    private final TradeAnalyzerService tradeAnalyzerService;
    private final TradeCostBasisAllocationService allocationService;
    private final PortfolioDisposalService disposalService;
    private final OwnedItemService ownedItemService;

    public TradeExecutionService(
            TradeTransactionRepository tradeTransactionRepository,
            TradeTransactionItemRepository tradeTransactionItemRepository,
            TradeAnalyzerService tradeAnalyzerService,
            TradeCostBasisAllocationService allocationService,
            PortfolioDisposalService disposalService,
            OwnedItemService ownedItemService) {
        this.tradeTransactionRepository = tradeTransactionRepository;
        this.tradeTransactionItemRepository = tradeTransactionItemRepository;
        this.tradeAnalyzerService = tradeAnalyzerService;
        this.allocationService = allocationService;
        this.disposalService = disposalService;
        this.ownedItemService = ownedItemService;
    }

    @Transactional
    public TradeTransaction execute(AppUser owner, Long tradeTransactionId) {
        TradeTransaction tradeTransaction = tradeTransactionRepository.findByIdAndOwner(tradeTransactionId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Trade transaction not found"));
        if (tradeTransaction.isExecuted()) {
            throw new IllegalArgumentException("Trade has already been executed");
        }
        if (tradeTransaction.isCancelled()) {
            throw new IllegalArgumentException("Cancelled trades cannot be executed");
        }

        tradeAnalyzerService.analyze(owner, tradeTransactionId);
        tradeTransaction = tradeTransactionRepository.findByIdAndOwner(tradeTransactionId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Trade transaction not found"));
        List<TradeTransactionItem> outgoingItems = tradeTransactionItemRepository
                .findByTradeTransactionAndSideTypeOrderByCreatedAtAscIdAsc(tradeTransaction, TradeSideType.OUTGOING);
        List<TradeTransactionItem> incomingItems = tradeTransactionItemRepository
                .findByTradeTransactionAndSideTypeOrderByCreatedAtAscIdAsc(tradeTransaction, TradeSideType.INCOMING);
        validateExecutable(outgoingItems, incomingItems);

        Map<TradeTransactionItem, BigDecimal> receivedAllocations = allocationService.allocateTradeValueReceived(
                outgoingItems,
                tradeTransaction.getTotalIncomingAdjustedValueSgd());
        Map<TradeTransactionItem, BigDecimal> incomingCostBasis = allocationService.allocateIncomingCostBasis(incomingItems);

        LocalDate executionDate = LocalDate.now();
        for (TradeTransactionItem outgoingItem : outgoingItems) {
            OwnedItem ownedItem = outgoingItem.getOutgoingOwnedItem();
            if (ownedItem.getStatus() != OwnedItemStatus.ACTIVE) {
                throw new IllegalArgumentException("Only active portfolio items can be traded");
            }
            OwnedItemDisposal disposal = disposalService.tradeAwayItemFromTransaction(
                    owner,
                    ownedItem.getId(),
                    receivedAllocations.get(outgoingItem),
                    executionDate,
                    transactionNote(tradeTransaction, outgoingItem),
                    tradeTransaction.getId());
            outgoingItem.linkDisposal(disposal);
        }

        for (TradeTransactionItem incomingItem : incomingItems) {
            BigDecimal costBasis = incomingCostBasis.get(incomingItem);
            OwnedItem incomingOwnedItem = incomingItem.getCard() == null
                    ? ownedItemService.addSealedProductToPortfolio(
                            owner,
                            incomingSealedOwnedItemForm(incomingItem, costBasis, executionDate, tradeTransaction.getId()))
                    : ownedItemService.addCardToPortfolio(
                            owner,
                            incomingCardOwnedItemForm(incomingItem, costBasis, executionDate, tradeTransaction.getId()));
            incomingItem.linkIncomingOwnedItem(incomingOwnedItem, costBasis);
        }

        tradeTransaction.markExecuted(OffsetDateTime.now());
        return tradeTransactionRepository.save(tradeTransaction);
    }

    private void validateExecutable(
            List<TradeTransactionItem> outgoingItems,
            List<TradeTransactionItem> incomingItems) {
        if (outgoingItems.isEmpty() || incomingItems.isEmpty()) {
            throw new IllegalArgumentException("A full trade transaction requires outgoing and incoming items");
        }
        for (TradeTransactionItem outgoingItem : outgoingItems) {
            if (outgoingItem.getOutgoingOwnedItem() == null) {
                throw new IllegalArgumentException("Outgoing trade item is missing a portfolio record");
            }
            if (outgoingItem.getOutgoingOwnedItem().getStatus() != OwnedItemStatus.ACTIVE) {
                throw new IllegalArgumentException("Only active portfolio items can be traded");
            }
        }
        for (TradeTransactionItem incomingItem : incomingItems) {
            if (incomingItem.getCard() == null && incomingItem.getSealedProduct() == null) {
                throw new IllegalArgumentException("Incoming trade item is missing an asset");
            }
        }
    }

    private OwnedItemForm incomingCardOwnedItemForm(
            TradeTransactionItem incomingItem,
            BigDecimal costBasis,
            LocalDate executionDate,
            Long tradeTransactionId) {
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(incomingItem.getCard().getId());
        form.setVariant(incomingItem.getIncomingVariant());
        form.setCondition(incomingItem.getIncomingCondition());
        form.setPurchasePriceSgd(costBasis);
        form.setPurchaseDate(executionDate);
        form.setGradedStatus(GradedStatus.UNGRADED);
        form.setNotes(incomingNote(incomingItem, tradeTransactionId));
        return form;
    }

    private OwnedItemForm incomingSealedOwnedItemForm(
            TradeTransactionItem incomingItem,
            BigDecimal costBasis,
            LocalDate executionDate,
            Long tradeTransactionId) {
        OwnedItemForm form = new OwnedItemForm();
        form.setSealedProductId(incomingItem.getSealedProduct().getId());
        form.setSealedCondition(incomingItem.getIncomingSealedCondition());
        form.setPurchasePriceSgd(costBasis);
        form.setPurchaseDate(executionDate);
        form.setNotes(incomingNote(incomingItem, tradeTransactionId));
        return form;
    }

    private String transactionNote(TradeTransaction tradeTransaction, TradeTransactionItem outgoingItem) {
        String note = "Executed via trade transaction #" + tradeTransaction.getId();
        if (outgoingItem.getNotes() == null || outgoingItem.getNotes().isBlank()) {
            return note;
        }
        return note + ". " + outgoingItem.getNotes().trim();
    }

    private String incomingNote(TradeTransactionItem incomingItem, Long tradeTransactionId) {
        String note = "Acquired via trade transaction #" + tradeTransactionId;
        if (incomingItem.getNotes() == null || incomingItem.getNotes().isBlank()) {
            return note;
        }
        return note + ". " + incomingItem.getNotes().trim();
    }
}
