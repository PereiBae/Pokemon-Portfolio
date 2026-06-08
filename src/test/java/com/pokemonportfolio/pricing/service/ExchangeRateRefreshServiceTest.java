package com.pokemonportfolio.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.provider.ExchangeRateProvider;
import com.pokemonportfolio.pricing.provider.ExchangeRateProviderException;
import com.pokemonportfolio.pricing.provider.ExchangeRateQuote;
import com.pokemonportfolio.pricing.repository.ExchangeRateSnapshotRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExchangeRateRefreshServiceTest {

    @Autowired
    private ExchangeRateRefreshService exchangeRateRefreshService;

    @Autowired
    private CurrencyConversionService currencyConversionService;

    @Autowired
    private ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;

    @MockBean
    private ExchangeRateProvider exchangeRateProvider;

    @Test
    void storesExchangeRateSnapshotFromEnabledProvider() {
        exchangeRateSnapshotRepository.deleteAll();
        when(exchangeRateProvider.isEnabled()).thenReturn(true);
        when(exchangeRateProvider.latestRate("EUR", "SGD"))
                .thenReturn(quote("EUR", "1.45670000"));

        var snapshot = exchangeRateRefreshService.refreshRateToSgd("eur");

        assertThat(snapshot.getSourceCurrency()).isEqualTo("EUR");
        assertThat(snapshot.getTargetCurrency()).isEqualTo("SGD");
        assertThat(snapshot.getExchangeRate()).isEqualByComparingTo("1.45670000");
        assertThat(snapshot.getRateSource()).isEqualTo("FRANKFURTER");
        assertThat(snapshot.getConfidenceRating()).isEqualTo(ConfidenceRating.MEDIUM);
        assertThat(currencyConversionService.latestRateToSgd("EUR"))
                .hasValueSatisfying(rate -> assertThat(rate).isEqualByComparingTo("1.45670000"));
    }

    @Test
    void duplicateSameProviderDateAndRateIsReused() {
        exchangeRateSnapshotRepository.deleteAll();
        when(exchangeRateProvider.isEnabled()).thenReturn(true);
        when(exchangeRateProvider.latestRate("USD", "SGD"))
                .thenReturn(quote("USD", "1.35120000"));

        ExchangeRateRefreshSummaryView first = exchangeRateRefreshService.refreshRatesToSgd(java.util.List.of("USD"));
        ExchangeRateRefreshSummaryView second = exchangeRateRefreshService.refreshRatesToSgd(java.util.List.of("USD"));

        assertThat(first.storedCount()).isEqualTo(1);
        assertThat(second.reusedCount()).isEqualTo(1);
        assertThat(exchangeRateSnapshotRepository.findAll())
                .filteredOn(snapshot -> "USD".equals(snapshot.getSourceCurrency()))
                .hasSize(1);
    }

    @Test
    void manualRateIsUsedWhenAutomaticProviderUnavailable() {
        exchangeRateSnapshotRepository.deleteAll();
        var manual = currencyConversionService.recordManualRate(
                "USD",
                new BigDecimal("1.35000000"),
                ConfidenceRating.LOW);
        when(exchangeRateProvider.isEnabled()).thenReturn(true);

        var resolved = exchangeRateRefreshService.ensureRateToSgd("USD");

        assertThat(resolved.getId()).isEqualTo(manual.getId());
        assertThat(resolved.getRateSource()).isEqualTo("MANUAL_STATIC");
        verify(exchangeRateProvider, never()).latestRate("USD", "SGD");
    }

    @Test
    void providerFailureReturnsFriendlyRefreshException() {
        exchangeRateSnapshotRepository.deleteAll();
        when(exchangeRateProvider.isEnabled()).thenReturn(true);
        when(exchangeRateProvider.latestRate("EUR", "SGD"))
                .thenThrow(new ExchangeRateProviderException("Provider unavailable."));

        assertThatThrownBy(() -> exchangeRateRefreshService.refreshRateToSgd("EUR"))
                .isInstanceOf(ExchangeRateRefreshException.class)
                .hasMessage("Could not fetch EUR to SGD exchange rate.");
    }

    private ExchangeRateQuote quote(String sourceCurrency, String rate) {
        return new ExchangeRateQuote(
                sourceCurrency,
                "SGD",
                new BigDecimal(rate),
                "FRANKFURTER",
                OffsetDateTime.parse("2026-06-07T00:00:00Z"),
                OffsetDateTime.parse("2026-06-08T01:30:00Z"));
    }
}
