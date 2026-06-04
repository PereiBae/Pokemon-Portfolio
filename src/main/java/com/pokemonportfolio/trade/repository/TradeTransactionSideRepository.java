package com.pokemonportfolio.trade.repository;

import com.pokemonportfolio.config.domain.TradeSideType;
import com.pokemonportfolio.trade.entity.TradeTransaction;
import com.pokemonportfolio.trade.entity.TradeTransactionSide;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeTransactionSideRepository extends JpaRepository<TradeTransactionSide, Long> {

    List<TradeTransactionSide> findByTradeTransaction(TradeTransaction tradeTransaction);

    Optional<TradeTransactionSide> findByTradeTransactionAndSideType(
            TradeTransaction tradeTransaction,
            TradeSideType sideType);
}
