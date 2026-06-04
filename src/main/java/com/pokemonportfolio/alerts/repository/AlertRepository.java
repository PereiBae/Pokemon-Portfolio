package com.pokemonportfolio.alerts.repository;

import com.pokemonportfolio.alerts.entity.Alert;
import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.config.domain.AlertStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    @EntityGraph(attributePaths = {"ownedItem.card.pokemonSet", "priceSnapshot"})
    List<Alert> findByOwnerAndStatusOrderByTriggeredAtDescIdDesc(AppUser owner, AlertStatus status);

    @EntityGraph(attributePaths = {"ownedItem.card.pokemonSet", "priceSnapshot"})
    List<Alert> findByOwnerOrderByTriggeredAtDescIdDesc(AppUser owner);

    @EntityGraph(attributePaths = {"ownedItem.card.pokemonSet", "priceSnapshot"})
    Optional<Alert> findByIdAndOwner(Long id, AppUser owner);

    List<Alert> findByOwnerAndOwnedItem_IdAndStatus(AppUser owner, Long ownedItemId, AlertStatus status);

    boolean existsByOwnedItemIdAndPriceSnapshotId(Long ownedItemId, Long priceSnapshotId);

    long countByOwnerAndStatus(AppUser owner, AlertStatus status);
}
