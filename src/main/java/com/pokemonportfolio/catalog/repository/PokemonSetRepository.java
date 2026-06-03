package com.pokemonportfolio.catalog.repository;

import com.pokemonportfolio.catalog.entity.PokemonSet;
import com.pokemonportfolio.config.domain.LanguageMarket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PokemonSetRepository extends JpaRepository<PokemonSet, Long> {

    Optional<PokemonSet> findByNameIgnoreCaseAndLanguageMarket(String name, LanguageMarket languageMarket);

    Optional<PokemonSet> findByExternalSetIdAndLanguageMarket(String externalSetId, LanguageMarket languageMarket);

    long countByExternalSetIdAndLanguageMarket(String externalSetId, LanguageMarket languageMarket);
}
