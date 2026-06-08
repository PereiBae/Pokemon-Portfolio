package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.entity.ExchangeRateSnapshot;
import com.pokemonportfolio.pricing.repository.ExchangeRateSnapshotRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrencyConversionService {

    public static final String SGD = "SGD";
    private static final BigDecimal ONE_TO_ONE_RATE = BigDecimal.ONE.setScale(8, RoundingMode.HALF_UP);

    private final ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;

    public CurrencyConversionService(ExchangeRateSnapshotRepository exchangeRateSnapshotRepository) {
        this.exchangeRateSnapshotRepository = exchangeRateSnapshotRepository;
    }

    public BigDecimal convertToSgd(BigDecimal sourcePrice, String sourceCurrency, BigDecimal exchangeRateUsed) {
        BigDecimal normalizedPrice = MoneyCalculationSupport.money(sourcePrice);
        BigDecimal normalizedRate = normalizeRate(normalizeCurrency(sourceCurrency), exchangeRateUsed);
        return MoneyCalculationSupport.money(normalizedPrice.multiply(normalizedRate));
    }

    public Optional<BigDecimal> latestRateToSgd(String sourceCurrency) {
        String normalizedCurrency = normalizeCurrency(sourceCurrency);
        if (SGD.equals(normalizedCurrency)) {
            return Optional.of(ONE_TO_ONE_RATE);
        }
        return exchangeRateSnapshotRepository
                .findTopBySourceCurrencyAndTargetCurrencyOrderByEffectiveAtDescIdDesc(normalizedCurrency, SGD)
                .map(ExchangeRateSnapshot::getExchangeRate)
                .map(rate -> normalizeRate(normalizedCurrency, rate));
    }

    public String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Source currency is required");
        }
        String normalized = currency.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter code");
        }
        return normalized;
    }

    public BigDecimal normalizeRate(String sourceCurrency, BigDecimal exchangeRateUsed) {
        if (SGD.equals(sourceCurrency) && exchangeRateUsed == null) {
            return ONE_TO_ONE_RATE;
        }
        if (exchangeRateUsed == null || exchangeRateUsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be greater than zero");
        }
        return exchangeRateUsed.setScale(8, RoundingMode.HALF_UP);
    }

    @Transactional
    public ExchangeRateSnapshot recordManualRate(
            String sourceCurrency,
            BigDecimal exchangeRateUsed,
            ConfidenceRating confidenceRating) {
        String normalizedCurrency = normalizeCurrency(sourceCurrency);
        BigDecimal normalizedRate = normalizeRate(normalizedCurrency, exchangeRateUsed);
        OffsetDateTime now = OffsetDateTime.now();
        return recordManualRate(normalizedCurrency, SGD, normalizedRate, confidenceRating, now);
    }

    @Transactional
    public ExchangeRateSnapshot recordManualRate(
            String sourceCurrency,
            String targetCurrency,
            BigDecimal exchangeRateUsed,
            ConfidenceRating confidenceRating,
            OffsetDateTime effectiveAt) {
        String normalizedSourceCurrency = normalizeCurrency(sourceCurrency);
        String normalizedTargetCurrency = normalizeCurrency(targetCurrency);
        if (!SGD.equals(normalizedTargetCurrency)) {
            throw new IllegalArgumentException("Only SGD target exchange rates are supported for v1");
        }
        BigDecimal normalizedRate = normalizeRate(normalizedSourceCurrency, exchangeRateUsed);
        OffsetDateTime effectiveTimestamp = effectiveAt == null ? OffsetDateTime.now() : effectiveAt;
        OffsetDateTime now = OffsetDateTime.now();
        return exchangeRateSnapshotRepository.save(new ExchangeRateSnapshot(
                normalizedSourceCurrency,
                normalizedTargetCurrency,
                normalizedRate,
                "MANUAL_STATIC",
                confidenceRating == null ? ConfidenceRating.LOW : confidenceRating,
                effectiveTimestamp,
                now));
    }

    @Transactional
    public ExchangeRateSnapshot recordProviderRate(
            String sourceCurrency,
            String targetCurrency,
            BigDecimal exchangeRateUsed,
            String rateSource,
            ConfidenceRating confidenceRating,
            OffsetDateTime effectiveAt,
            OffsetDateTime fetchedAt) {
        String normalizedSourceCurrency = normalizeCurrency(sourceCurrency);
        String normalizedTargetCurrency = normalizeCurrency(targetCurrency);
        if (!SGD.equals(normalizedTargetCurrency)) {
            throw new IllegalArgumentException("Only SGD target exchange rates are supported for v1");
        }
        if (rateSource == null || rateSource.isBlank()) {
            throw new IllegalArgumentException("Exchange-rate source is required");
        }
        BigDecimal normalizedRate = normalizeRate(normalizedSourceCurrency, exchangeRateUsed);
        OffsetDateTime effectiveTimestamp = effectiveAt == null ? OffsetDateTime.now() : effectiveAt;
        OffsetDateTime fetchedTimestamp = fetchedAt == null ? OffsetDateTime.now() : fetchedAt;
        return exchangeRateSnapshotRepository.save(new ExchangeRateSnapshot(
                normalizedSourceCurrency,
                normalizedTargetCurrency,
                normalizedRate,
                rateSource.trim().toUpperCase(),
                confidenceRating == null ? ConfidenceRating.MEDIUM : confidenceRating,
                effectiveTimestamp,
                fetchedTimestamp));
    }
}
