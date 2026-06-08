package com.pokemonportfolio.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.repository.ExchangeRateSnapshotRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CurrencyConversionServiceTest {

    @Autowired
    private CurrencyConversionService currencyConversionService;

    @Autowired
    private ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;

    @Test
    void convertsSourceCurrencyToSgdUsingManualRate() {
        BigDecimal converted = currencyConversionService.convertToSgd(
                new BigDecimal("10.00"),
                "usd",
                new BigDecimal("1.35000000"));

        assertThat(converted).isEqualByComparingTo("13.50");
    }

    @Test
    void recordsManualExchangeRateSnapshot() {
        var snapshot = currencyConversionService.recordManualRate(
                "usd",
                new BigDecimal("1.35000000"),
                ConfidenceRating.LOW);

        assertThat(snapshot.getId()).isNotNull();
        assertThat(snapshot.getSourceCurrency()).isEqualTo("USD");
        assertThat(snapshot.getTargetCurrency()).isEqualTo("SGD");
        assertThat(snapshot.getExchangeRate()).isEqualByComparingTo("1.35000000");
        assertThat(snapshot.getRateSource()).isEqualTo("MANUAL_STATIC");
        assertThat(exchangeRateSnapshotRepository.findById(snapshot.getId())).isPresent();
    }

    @Test
    void recordsAndUsesEurToSgdExchangeRateSnapshot() {
        currencyConversionService.recordManualRate(
                "eur",
                new BigDecimal("1.46000000"),
                ConfidenceRating.LOW);

        assertThat(currencyConversionService.latestRateToSgd("EUR"))
                .hasValueSatisfying(rate -> assertThat(rate).isEqualByComparingTo("1.46000000"));
        assertThat(currencyConversionService.convertToSgd(
                new BigDecimal("120.00"),
                "EUR",
                currencyConversionService.latestRateToSgd("EUR").orElseThrow()))
                .isEqualByComparingTo("175.20");
    }
}
