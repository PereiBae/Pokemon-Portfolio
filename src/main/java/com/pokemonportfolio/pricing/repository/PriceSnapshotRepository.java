package com.pokemonportfolio.pricing.repository;

import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    Optional<PriceSnapshot> findTopByCardIdOrderByCalculatedAtDescIdDesc(Long cardId);

    Optional<PriceSnapshot> findTopByCardIdAndCardVariantOrderByCalculatedAtDescIdDesc(
            Long cardId,
            CardVariant cardVariant);

    Optional<PriceSnapshot> findTopByCardIdAndCardVariantIsNullOrderByCalculatedAtDescIdDesc(Long cardId);

    List<PriceSnapshot> findByCardIdOrderByCalculatedAtDescIdDesc(Long cardId);

    @Query("""
            select snapshot
            from PriceSnapshot snapshot
            where snapshot.card.id = :cardId
              and (snapshot.cardVariant = :cardVariant or snapshot.cardVariant is null)
            order by snapshot.calculatedAt asc, snapshot.id asc
            """)
    List<PriceSnapshot> findCardHistoryForOwnedVariant(
            @Param("cardId") Long cardId,
            @Param("cardVariant") CardVariant cardVariant);

    @Query("""
            select snapshot
            from PriceSnapshot snapshot
            where snapshot.card.id = :cardId
              and (snapshot.cardVariant = :cardVariant or snapshot.cardVariant is null)
              and snapshot.calculatedAt >= :since
            order by snapshot.calculatedAt asc, snapshot.id asc
            """)
    List<PriceSnapshot> findCardHistoryForOwnedVariantSince(
            @Param("cardId") Long cardId,
            @Param("cardVariant") CardVariant cardVariant,
            @Param("since") OffsetDateTime since);

    Optional<PriceSnapshot> findTopBySealedProductIdOrderByCalculatedAtDescIdDesc(Long sealedProductId);

    List<PriceSnapshot> findBySealedProductIdOrderByCalculatedAtDescIdDesc(Long sealedProductId);

    List<PriceSnapshot> findBySealedProductIdOrderByCalculatedAtAscIdAsc(Long sealedProductId);

    List<PriceSnapshot> findBySealedProductIdAndCalculatedAtGreaterThanEqualOrderByCalculatedAtAscIdAsc(
            Long sealedProductId,
            OffsetDateTime calculatedAt);
}
