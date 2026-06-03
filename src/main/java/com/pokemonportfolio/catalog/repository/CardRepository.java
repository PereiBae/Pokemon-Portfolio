package com.pokemonportfolio.catalog.repository;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.LanguageMarket;
import java.util.List;
import java.util.Optional;
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

    Optional<Card> findByCatalogSourceAndExternalCardId(CatalogSource catalogSource, String externalCardId);

    @Query("""
            select c
            from Card c
            join c.pokemonSet s
            where lower(c.name) = lower(:name)
              and c.cardNumber = :cardNumber
              and c.languageMarket = :languageMarket
              and c.variant = :variant
              and lower(s.name) = lower(:setName)
            """)
    Optional<Card> findByIdentity(
            @Param("name") String name,
            @Param("setName") String setName,
            @Param("cardNumber") String cardNumber,
            @Param("languageMarket") LanguageMarket languageMarket,
            @Param("variant") CardVariant variant);
}
