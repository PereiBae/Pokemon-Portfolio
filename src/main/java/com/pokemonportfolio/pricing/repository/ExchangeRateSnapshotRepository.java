package com.pokemonportfolio.pricing.repository;

import com.pokemonportfolio.pricing.entity.ExchangeRateSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateSnapshotRepository extends JpaRepository<ExchangeRateSnapshot, Long> {

    Optional<ExchangeRateSnapshot> findTopBySourceCurrencyAndTargetCurrencyOrderByEffectiveAtDescIdDesc(
            String sourceCurrency,
            String targetCurrency);
}
