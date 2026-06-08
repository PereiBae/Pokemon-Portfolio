package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.PricingResultType;
import com.pokemonportfolio.pricing.entity.PricingProviderResult;
import com.pokemonportfolio.pricing.provider.PricingProviderResultValue;
import com.pokemonportfolio.pricing.repository.PricingProviderResultRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingProviderResultService {

    private final PricingProviderResultRepository repository;

    public PricingProviderResultService(PricingProviderResultRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<PricingProviderResult> storeCardResults(
            Card card,
            CardVariant variant,
            List<PricingProviderResultValue> values) {
        OffsetDateTime capturedAt = OffsetDateTime.now();
        return repository.saveAll(values.stream()
                .map(value -> new PricingProviderResult(
                        card,
                        variant,
                        value.providerName(),
                        value.resultType(),
                        value.sourceMarket(),
                        MoneyCalculationSupport.money(value.sourcePrice()),
                        value.sourceCurrency(),
                        value.exchangeRateUsed(),
                        MoneyCalculationSupport.money(value.priceSgd()),
                        value.confidenceRating(),
                        value.sampleSize(),
                        value.sourceUrl(),
                        value.sourceUpdatedAt(),
                        value.providerMetadata(),
                        capturedAt))
                .toList());
    }

    @Transactional(readOnly = true)
    public Optional<PricingProviderResult> latestCardResult(
            Long cardId,
            CardVariant variant,
            PricingResultType resultType) {
        return repository.findTopByCardIdAndCardVariantAndResultTypeOrderByCapturedAtDescIdDesc(
                cardId,
                variant,
                resultType);
    }
}
