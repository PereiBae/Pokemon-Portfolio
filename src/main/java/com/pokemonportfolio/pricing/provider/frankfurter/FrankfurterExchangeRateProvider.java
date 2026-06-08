package com.pokemonportfolio.pricing.provider.frankfurter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pokemonportfolio.pricing.provider.ExchangeRateProvider;
import com.pokemonportfolio.pricing.provider.ExchangeRateProviderException;
import com.pokemonportfolio.pricing.provider.ExchangeRateQuote;
import com.pokemonportfolio.pricing.service.CurrencyConversionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Order(10)
public class FrankfurterExchangeRateProvider implements ExchangeRateProvider {

    public static final String PROVIDER_NAME = "FRANKFURTER";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final FrankfurterExchangeRateProperties properties;
    private final CurrencyConversionService currencyConversionService;
    private final WebClient webClient;

    public FrankfurterExchangeRateProvider(
            FrankfurterExchangeRateProperties properties,
            CurrencyConversionService currencyConversionService,
            WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.currencyConversionService = currencyConversionService;
        this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public ExchangeRateQuote latestRate(String sourceCurrency, String targetCurrency) {
        if (!properties.isEnabled()) {
            throw new ExchangeRateProviderException("Frankfurter exchange-rate provider is disabled.");
        }
        String normalizedSource = currencyConversionService.normalizeCurrency(sourceCurrency);
        String normalizedTarget = currencyConversionService.normalizeCurrency(targetCurrency);
        try {
            FrankfurterLatestResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/latest")
                            .queryParam("base", normalizedSource)
                            .queryParam("symbols", normalizedTarget)
                            .build())
                    .retrieve()
                    .bodyToMono(FrankfurterLatestResponse.class)
                    .block(REQUEST_TIMEOUT);
            return toQuote(response, normalizedSource, normalizedTarget);
        } catch (ExchangeRateProviderException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ExchangeRateProviderException(
                    "Frankfurter exchange-rate lookup failed for "
                            + normalizedSource + " to " + normalizedTarget + ".",
                    ex);
        }
    }

    private ExchangeRateQuote toQuote(
            FrankfurterLatestResponse response,
            String requestedSourceCurrency,
            String requestedTargetCurrency) {
        if (response == null || response.rates() == null) {
            throw new ExchangeRateProviderException("Frankfurter exchange-rate response was empty.");
        }
        BigDecimal targetRate = response.rates().get(requestedTargetCurrency);
        if (targetRate == null) {
            throw new ExchangeRateProviderException(
                    "Frankfurter response did not include " + requestedTargetCurrency + ".");
        }
        BigDecimal amount = response.amount() == null ? BigDecimal.ONE : response.amount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExchangeRateProviderException("Frankfurter response amount must be greater than zero.");
        }
        BigDecimal exchangeRate = targetRate
                .divide(amount, 8, RoundingMode.HALF_UP);
        LocalDate effectiveDate = response.date() == null ? LocalDate.now(ZoneOffset.UTC) : response.date();
        OffsetDateTime effectiveAt = effectiveDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        return new ExchangeRateQuote(
                requestedSourceCurrency,
                requestedTargetCurrency,
                exchangeRate,
                PROVIDER_NAME,
                effectiveAt,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FrankfurterLatestResponse(
            BigDecimal amount,
            String base,
            LocalDate date,
            Map<String, BigDecimal> rates) {
    }
}
