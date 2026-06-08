package com.pokemonportfolio.pricing.repository;

import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    Optional<PriceSnapshot> findTopByCardIdOrderByCalculatedAtDescIdDesc(Long cardId);

    Optional<PriceSnapshot> findTopByCardIdAndCardVariantOrderByCalculatedAtDescIdDesc(
            Long cardId,
            CardVariant cardVariant);

    Optional<PriceSnapshot> findTopByCardIdAndCardVariantIsNullOrderByCalculatedAtDescIdDesc(Long cardId);

    List<PriceSnapshot> findByCardIdOrderByCalculatedAtDescIdDesc(Long cardId);

    Optional<PriceSnapshot> findTopBySealedProductIdOrderByCalculatedAtDescIdDesc(Long sealedProductId);

    List<PriceSnapshot> findBySealedProductIdOrderByCalculatedAtDescIdDesc(Long sealedProductId);
}
