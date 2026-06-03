package com.pokemonportfolio.portfolio.repository;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedItemRepository extends JpaRepository<OwnedItem, Long> {

    List<OwnedItem> findByOwnerAndArchivedAtIsNullOrderByCreatedAtDesc(AppUser owner);

    Optional<OwnedItem> findByIdAndOwnerAndArchivedAtIsNull(Long id, AppUser owner);

    long countByCardId(Long cardId);
}
