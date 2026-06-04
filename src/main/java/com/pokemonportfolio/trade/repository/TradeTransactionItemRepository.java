package com.pokemonportfolio.trade.repository;

import com.pokemonportfolio.config.domain.TradeSideType;
import com.pokemonportfolio.trade.entity.TradeTransaction;
import com.pokemonportfolio.trade.entity.TradeTransactionItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeTransactionItemRepository extends JpaRepository<TradeTransactionItem, Long> {

    @EntityGraph(attributePaths = {
            "card",
            "card.pokemonSet",
            "sealedProduct",
            "outgoingOwnedItem",
            "outgoingOwnedItem.card",
            "outgoingOwnedItem.card.pokemonSet",
            "outgoingOwnedItem.sealedProduct",
            "incomingOwnedItem",
            "incomingOwnedItem.card",
            "incomingOwnedItem.card.pokemonSet",
            "incomingOwnedItem.sealedProduct",
            "disposal"
    })
    List<TradeTransactionItem> findByTradeTransactionOrderByCreatedAtAscIdAsc(TradeTransaction tradeTransaction);

    @EntityGraph(attributePaths = {
            "card",
            "card.pokemonSet",
            "sealedProduct",
            "outgoingOwnedItem",
            "outgoingOwnedItem.card",
            "outgoingOwnedItem.card.pokemonSet",
            "outgoingOwnedItem.sealedProduct",
            "incomingOwnedItem",
            "incomingOwnedItem.card",
            "incomingOwnedItem.card.pokemonSet",
            "incomingOwnedItem.sealedProduct",
            "disposal"
    })
    List<TradeTransactionItem> findByTradeTransactionAndSideTypeOrderByCreatedAtAscIdAsc(
            TradeTransaction tradeTransaction,
            TradeSideType sideType);

    boolean existsByTradeTransactionAndOutgoingOwnedItemId(TradeTransaction tradeTransaction, Long outgoingOwnedItemId);
}
