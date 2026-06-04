package com.pokemonportfolio.trade.repository;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.trade.entity.TradeTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeTransactionRepository extends JpaRepository<TradeTransaction, Long> {

    List<TradeTransaction> findByOwnerOrderByCreatedAtDesc(AppUser owner);

    Optional<TradeTransaction> findByIdAndOwner(Long id, AppUser owner);
}
