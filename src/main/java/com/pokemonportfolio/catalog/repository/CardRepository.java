package com.pokemonportfolio.catalog.repository;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.LanguageMarket;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByLanguageMarketAndActiveTrueOrderByNameAsc(LanguageMarket languageMarket);

    List<Card> findByLanguageMarketAndNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(
            LanguageMarket languageMarket,
            String name);

    @EntityGraph(attributePaths = "pokemonSet")
    @Query("select c from Card c where c.languageMarket = :languageMarket and c.active = true order by c.name asc")
    List<Card> findActiveByLanguageMarketWithSet(@Param("languageMarket") LanguageMarket languageMarket);
}
