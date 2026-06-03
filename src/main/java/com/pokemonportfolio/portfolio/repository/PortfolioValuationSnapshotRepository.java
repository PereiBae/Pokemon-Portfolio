package com.pokemonportfolio.portfolio.repository;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.portfolio.entity.PortfolioValuationSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioValuationSnapshotRepository extends JpaRepository<PortfolioValuationSnapshot, Long> {

    Optional<PortfolioValuationSnapshot> findTopByOwnerOrderByCalculatedAtDescIdDesc(AppUser owner);

    List<PortfolioValuationSnapshot> findByOwnerOrderByCalculatedAtAscIdAsc(AppUser owner);
}

