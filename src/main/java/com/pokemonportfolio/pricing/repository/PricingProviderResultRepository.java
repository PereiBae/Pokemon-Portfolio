package com.pokemonportfolio.pricing.repository;

import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.PricingResultType;
import com.pokemonportfolio.pricing.entity.PricingProviderResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingProviderResultRepository extends JpaRepository<PricingProviderResult, Long> {

    Optional<PricingProviderResult> findTopByCardIdAndCardVariantAndResultTypeOrderByCapturedAtDescIdDesc(
            Long cardId,
            CardVariant cardVariant,
            PricingResultType resultType);

    List<PricingProviderResult> findByCardIdOrderByCapturedAtDescIdDesc(Long cardId);
}
