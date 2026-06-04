package com.pokemonportfolio.portfolio.repository;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.DisposalType;
import com.pokemonportfolio.portfolio.entity.OwnedItemDisposal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedItemDisposalRepository extends JpaRepository<OwnedItemDisposal, Long> {

    @EntityGraph(attributePaths = {"ownedItem", "ownedItem.card", "ownedItem.card.pokemonSet"})
    List<OwnedItemDisposal> findByOwnerOrderByDisposalDateDescIdDesc(AppUser owner);

    List<OwnedItemDisposal> findByOwnerAndDisposalTypeIn(AppUser owner, List<DisposalType> disposalTypes);

    Optional<OwnedItemDisposal> findByOwnedItemId(Long ownedItemId);
}
