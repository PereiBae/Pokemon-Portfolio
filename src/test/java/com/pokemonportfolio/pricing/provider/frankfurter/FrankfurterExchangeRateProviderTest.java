package com.pokemonportfolio.pricing.provider.frankfurter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.pokemonportfolio.pricing.provider.ExchangeRateQuote;
import com.pokemonportfolio.pricing.repository.ExchangeRateSnapshotRepository;
import com.pokemonportfolio.pricing.service.CurrencyConversionService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class FrankfurterExchangeRateProviderTest {

    private HttpServer server;
    private FrankfurterExchangeRateProperties properties;
    private FrankfurterExchangeRateProvider provider;
    private String lastQuery;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        properties = new FrankfurterExchangeRateProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        provider = new FrankfurterExchangeRateProvider(
                properties,
                new CurrencyConversionService(mock(ExchangeRateSnapshotRepository.class)),
                WebClient.builder());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void mapsEurToSgdLatestResponse() {
        respondWith("""
                {"amount":1.0,"base":"EUR","date":"2026-06-07","rates":{"SGD":1.4567}}
                """);

        ExchangeRateQuote quote = provider.latestRate("eur", "sgd");

        assertThat(lastQuery).contains("base=EUR", "symbols=SGD");
        assertThat(quote.sourceCurrency()).isEqualTo("EUR");
        assertThat(quote.targetCurrency()).isEqualTo("SGD");
        assertThat(quote.exchangeRate()).isEqualByComparingTo("1.45670000");
        assertThat(quote.rateSource()).isEqualTo("FRANKFURTER");
        assertThat(quote.effectiveAt().toLocalDate()).isEqualTo(java.time.LocalDate.of(2026, 6, 7));
    }

    @Test
    void mapsUsdToSgdLatestResponse() {
        respondWith("""
                {"amount":1.0,"base":"USD","date":"2026-06-07","rates":{"SGD":1.3512}}
                """);

        ExchangeRateQuote quote = provider.latestRate("USD", "SGD");

        assertThat(lastQuery).contains("base=USD", "symbols=SGD");
        assertThat(quote.sourceCurrency()).isEqualTo("USD");
        assertThat(quote.targetCurrency()).isEqualTo("SGD");
        assertThat(quote.exchangeRate()).isEqualByComparingTo("1.35120000");
        assertThat(quote.rateSource()).isEqualTo("FRANKFURTER");
    }

    private void respondWith(String body) {
        server.createContext("/v1/latest", exchange -> {
            lastQuery = exchange.getRequestURI().getQuery();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

}
