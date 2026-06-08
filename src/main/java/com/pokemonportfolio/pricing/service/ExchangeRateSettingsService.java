package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.entity.ExchangeRateSnapshot;
import com.pokemonportfolio.pricing.repository.ExchangeRateSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExchangeRateSettingsService {

    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    private final CurrencyConversionService currencyConversionService;
    private final ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;

    public ExchangeRateSettingsService(
            CurrencyConversionService currencyConversionService,
            ExchangeRateSnapshotRepository exchangeRateSnapshotRepository) {
        this.currencyConversionService = currencyConversionService;
        this.exchangeRateSnapshotRepository = exchangeRateSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateView> listRates() {
        List<ExchangeRateSnapshot> snapshots = exchangeRateSnapshotRepository.findAll().stream()
                .sorted(Comparator.comparing(ExchangeRateSnapshot::getSourceCurrency)
                        .thenComparing(ExchangeRateSnapshot::getTargetCurrency)
                        .thenComparing(ExchangeRateSnapshot::getEffectiveAt, Comparator.reverseOrder())
                        .thenComparing(ExchangeRateSnapshot::getId, Comparator.reverseOrder()))
                .toList();
        Set<String> activePairs = new HashSet<>();
        return snapshots.stream()
                .map(snapshot -> toView(snapshot, activePairs.add(pairKey(snapshot))))
                .toList();
    }

    @Transactional
    public ExchangeRateSnapshot recordManualRate(ExchangeRateForm form) {
        String sourceCurrency = currencyConversionService.normalizeCurrency(form.getSourceCurrency());
        String targetCurrency = currencyConversionService.normalizeCurrency(form.getTargetCurrency());
        if (!CurrencyConversionService.SGD.equals(targetCurrency)) {
            throw new IllegalArgumentException("Only SGD target exchange rates are supported for v1.");
        }
        if (!form.isActive()) {
            throw new IllegalArgumentException("Only active latest rates are supported for v1. Add a new active rate to replace the prior one.");
        }
        BigDecimal normalizedRate = currencyConversionService.normalizeRate(sourceCurrency, form.getExchangeRate());
        OffsetDateTime effectiveAt = effectiveAt(form.getEffectiveDate());
        return currencyConversionService.recordManualRate(
                sourceCurrency,
                targetCurrency,
                normalizedRate,
                ConfidenceRating.LOW,
                effectiveAt);
    }

    private OffsetDateTime effectiveAt(LocalDate effectiveDate) {
        LocalDate date = effectiveDate == null ? LocalDate.now(SINGAPORE) : effectiveDate;
        return date.atStartOfDay(SINGAPORE).toOffsetDateTime();
    }

    private ExchangeRateView toView(ExchangeRateSnapshot snapshot, boolean active) {
        return new ExchangeRateView(
                snapshot.getId(),
                snapshot.getSourceCurrency(),
                snapshot.getTargetCurrency(),
                snapshot.getExchangeRate(),
                snapshot.getRateSource(),
                snapshot.getConfidenceRating(),
                snapshot.getEffectiveAt(),
                snapshot.getFetchedAt(),
                active);
    }

    private String pairKey(ExchangeRateSnapshot snapshot) {
        return snapshot.getSourceCurrency() + "->" + snapshot.getTargetCurrency();
    }
}
