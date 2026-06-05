package com.pokemonportfolio.grading.repository;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.grading.entity.GradingAnalysis;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradingAnalysisRepository extends JpaRepository<GradingAnalysis, Long> {

    @EntityGraph(attributePaths = {
            "ownedItem",
            "ownedItem.card",
            "ownedItem.card.pokemonSet",
            "gradingFee"
    })
    Optional<GradingAnalysis> findByIdAndOwner(Long id, AppUser owner);

    @EntityGraph(attributePaths = {
            "ownedItem",
            "ownedItem.card",
            "ownedItem.card.pokemonSet",
            "gradingFee"
    })
    List<GradingAnalysis> findByOwnerOrderByCreatedAtDesc(AppUser owner);
}
