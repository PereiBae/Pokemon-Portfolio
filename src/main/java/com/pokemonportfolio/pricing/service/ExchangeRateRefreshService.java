package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.entity.ExchangeRateSnapshot;
import com.pokemonportfolio.pricing.provider.ExchangeRateProvider;
import com.pokemonportfolio.pricing.provider.ExchangeRateProviderException;
import com.pokemonportfolio.pricing.provider.ExchangeRateQuote;
import com.pokemonportfolio.pricing.repository.ExchangeRateSnapshotRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateRefreshService {

    public static final List<String> DEFAULT_SOURCE_CURRENCIES = List.of("USD", "EUR");

    private final List<ExchangeRateProvider> providers;
    private final CurrencyConversionService currencyConversionService;
    private final ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;

    public ExchangeRateRefreshService(
            List<ExchangeRateProvider> providers,
            CurrencyConversionService currencyConversionService,
            ExchangeRateSnapshotRepository exchangeRateSnapshotRepository) {
        this.providers = List.copyOf(providers);
        this.currencyConversionService = currencyConversionService;
        this.exchangeRateSnapshotRepository = exchangeRateSnapshotRepository;
    }

    public ExchangeRateRefreshSummaryView refreshDefaultSgdRates() {
        return refreshRatesToSgd(DEFAULT_SOURCE_CURRENCIES);
    }

    public ExchangeRateRefreshSummaryView refreshRatesToSgd(Collection<String> sourceCurrencies) {
        List<String> messages = new ArrayList<>();
        int stored = 0;
        int reused = 0;
        int failed = 0;
        List<String> normalizedCurrencies = sourceCurrencies == null
                ? List.of()
                : sourceCurrencies.stream()
                        .map(currencyConversionService::normalizeCurrency)
                        .distinct()
                        .filter(currency -> !CurrencyConversionService.SGD.equals(currency))
                        .toList();

        for (String sourceCurrency : normalizedCurrencies) {
            try {
                StoreResult result = refreshRateToSgdInternal(sourceCurrency);
                if (result.created()) {
                    stored++;
                    messages.add(sourceCurrency + " to SGD stored from " + result.snapshot().getRateSource() + ".");
                } else {
                    reused++;
                    messages.add(sourceCurrency + " to SGD already current from " + result.snapshot().getRateSource() + ".");
                }
            } catch (ExchangeRateRefreshException ex) {
                failed++;
                messages.add(ex.getMessage());
            }
        }

        return new ExchangeRateRefreshSummaryView(normalizedCurrencies.size(), stored, reused, failed, messages);
    }

    public ExchangeRateSnapshot ensureRateToSgd(String sourceCurrency) {
        String normalizedCurrency = currencyConversionService.normalizeCurrency(sourceCurrency);
        if (CurrencyConversionService.SGD.equals(normalizedCurrency)) {
            throw new ExchangeRateRefreshException("SGD does not require an exchange-rate refresh.");
        }
        return exchangeRateSnapshotRepository
                .findTopBySourceCurrencyAndTargetCurrencyOrderByEffectiveAtDescIdDesc(
                        normalizedCurrency,
                        CurrencyConversionService.SGD)
                .orElseGet(() -> refreshRateToSgd(normalizedCurrency));
    }

    public ExchangeRateSnapshot refreshRateToSgd(String sourceCurrency) {
        return refreshRateToSgdInternal(sourceCurrency).snapshot();
    }

    private StoreResult refreshRateToSgdInternal(String sourceCurrency) {
        String normalizedCurrency = currencyConversionService.normalizeCurrency(sourceCurrency);
        if (CurrencyConversionService.SGD.equals(normalizedCurrency)) {
            throw new ExchangeRateRefreshException("SGD does not require an exchange-rate refresh.");
        }
        ExchangeRateProvider provider = enabledProvider();
        try {
            ExchangeRateQuote quote = provider.latestRate(normalizedCurrency, CurrencyConversionService.SGD);
            if (quote == null) {
                throw new ExchangeRateProviderException("Exchange-rate provider returned no quote.");
            }
            return storeQuote(quote);
        } catch (ExchangeRateProviderException ex) {
            throw new ExchangeRateRefreshException(
                    "Could not fetch " + normalizedCurrency + " to SGD exchange rate.",
                    ex);
        }
    }

    private ExchangeRateProvider enabledProvider() {
        return providers.stream()
                .filter(ExchangeRateProvider::isEnabled)
                .findFirst()
                .orElseThrow(() -> new ExchangeRateRefreshException("No enabled exchange-rate provider is configured."));
    }

    private StoreResult storeQuote(ExchangeRateQuote quote) {
        String sourceCurrency = currencyConversionService.normalizeCurrency(quote.sourceCurrency());
        String targetCurrency = currencyConversionService.normalizeCurrency(quote.targetCurrency());
        String rateSource = quote.rateSource() == null ? "" : quote.rateSource().trim().toUpperCase();
        ExchangeRateSnapshot existing = exchangeRateSnapshotRepository
                .findTopBySourceCurrencyAndTargetCurrencyAndRateSourceOrderByEffectiveAtDescIdDesc(
                        sourceCurrency,
                        targetCurrency,
                        rateSource)
                .filter(snapshot -> sameProviderSnapshot(snapshot, quote))
                .orElse(null);
        if (existing != null) {
            return new StoreResult(existing, false);
        }
        ExchangeRateSnapshot snapshot = currencyConversionService.recordProviderRate(
                sourceCurrency,
                targetCurrency,
                quote.exchangeRate(),
                rateSource,
                ConfidenceRating.MEDIUM,
                quote.effectiveAt(),
                quote.fetchedAt());
        return new StoreResult(snapshot, true);
    }

    private boolean sameProviderSnapshot(ExchangeRateSnapshot snapshot, ExchangeRateQuote quote) {
        if (quote.effectiveAt() == null) {
            return false;
        }
        return snapshot.getEffectiveAt().toLocalDate().equals(quote.effectiveAt().toLocalDate())
                && snapshot.getExchangeRate().compareTo(quote.exchangeRate()) == 0;
    }

    private record StoreResult(ExchangeRateSnapshot snapshot, boolean created) {
    }
}
