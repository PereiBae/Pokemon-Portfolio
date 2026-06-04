package com.pokemonportfolio.portfolio.repository;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedItemRepository extends JpaRepository<OwnedItem, Long> {

    @EntityGraph(attributePaths = {"card", "card.pokemonSet"})
    List<OwnedItem> findByOwnerAndStatusOrderByCreatedAtDesc(AppUser owner, OwnedItemStatus status);

    @EntityGraph(attributePaths = {"card", "card.pokemonSet"})
    Optional<OwnedItem> findByIdAndOwnerAndStatus(Long id, AppUser owner, OwnedItemStatus status);

    @EntityGraph(attributePaths = {"card", "card.pokemonSet"})
    Optional<OwnedItem> findByIdAndOwner(Long id, AppUser owner);

    long countByCardId(Long cardId);
}
