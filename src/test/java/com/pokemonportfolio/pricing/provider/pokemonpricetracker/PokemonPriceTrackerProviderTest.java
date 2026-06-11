package com.pokemonportfolio.pricing.provider.pokemonpricetracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class PokemonPriceTrackerProviderTest {

    @Test
    void disabledProviderDoesNotCallApi() {
        PokemonPriceTrackerPricingProperties properties = new PokemonPriceTrackerPricingProperties();
        AtomicInteger calls = new AtomicInteger();
        PokemonPriceTrackerProvider provider = new PokemonPriceTrackerProvider(
                properties,
                WebClient.builder().exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.empty();
                }));

        assertThat(provider.isEnabled()).isFalse();
        assertThatThrownBy(() -> provider.fetchOneCard("490294"))
                .isInstanceOf(PokemonPriceTrackerProviderException.class)
                .hasMessageContaining("disabled");
        assertThat(calls).hasValue(0);
    }

    @Test
    void enabledProviderRequiresApiKeyBeforeCallingApi() {
        PokemonPriceTrackerPricingProperties properties = enabledProperties();
        properties.setApiKey("");
        AtomicInteger calls = new AtomicInteger();
        PokemonPriceTrackerProvider provider = new PokemonPriceTrackerProvider(
                properties,
                WebClient.builder().exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.empty();
                }));

        assertThatThrownBy(() -> provider.fetchOneCard("490294"))
                .isInstanceOf(PokemonPriceTrackerProviderException.class)
                .hasMessageContaining("API key");
        assertThat(calls).hasValue(0);
    }

    @Test
    void fetchCardSendsBearerTokenAndMapsRawAndPsaPrices() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        PokemonPriceTrackerProvider provider = providerWithResponse(capturedRequest, HttpStatus.OK, """
                {
                  "data": [
                    {
                      "tcgPlayerId": 490294,
                      "name": "Charizard ex",
                      "setName": "Obsidian Flames",
                      "cardNumber": "223",
                      "language": "English",
                      "printing": "Holofoil",
                      "imageUrl": "https://images.example/charizard.png",
                      "currency": "USD",
                      "marketPrice": 84.75,
                      "lowPrice": 78.00,
                      "averagePrice": 86.10,
                      "highPrice": 98.00,
                      "sellers": 21,
                      "lastPriceUpdate": "2026-06-08T02:15:00Z",
                      "ebay": {
                        "psa": {
                          "8": { "medianPrice": 100.00, "sampleSize": 7, "currency": "USD" },
                          "9": { "medianPrice": 140.00, "sampleSize": 8, "currency": "USD" },
                          "10": { "medianPrice": 315.00, "sampleSize": 12, "currency": "USD" }
                        }
                      }
                    }
                  ]
                }
                """);

        var card = provider.fetchOneCard("490294");

        assertThat(card.providerCardId()).isEqualTo("490294");
        assertThat(card.cardName()).isEqualTo("Charizard ex");
        assertThat(card.expansionName()).isEqualTo("Obsidian Flames");
        assertThat(card.cardNumber()).isEqualTo("223");
        assertThat(card.printVariant()).isEqualTo("Holofoil");
        assertThat(card.displayRawPrice()).isEqualByComparingTo("84.75");
        assertThat(card.sourceMarket()).isEqualTo("TCGPLAYER");
        assertThat(card.sourceCurrency()).isEqualTo("USD");
        assertThat(card.saleCount()).isEqualTo(21);
        assertThat(card.psa8Price().displayPrice()).isEqualByComparingTo("100.00");
        assertThat(card.psa9Price().displayPrice()).isEqualByComparingTo("140.00");
        assertThat(card.psa10Price().displayPrice()).isEqualByComparingTo("315.00");
        assertThat(capturedRequest.get().headers().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer test-ppt-key");
        assertThat(capturedRequest.get().url().getQuery())
                .contains("tcgPlayerId=490294", "includeBoth=true", "includeEbay=true", "days=90");
    }

    @Test
    void mapsNestedPricesMarketShapeFromLiveCardResponse() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        PokemonPriceTrackerProvider provider = providerWithResponse(capturedRequest, HttpStatus.OK, """
                {
                  "data": [
                    {
                      "id": 517046,
                      "name": "Blastoise ex - 200/165",
                      "setName": "SV: Scarlet & Violet 151",
                      "number": "200/165",
                      "image": "https://images.example/blastoise.png",
                      "prices": {
                        "market": 54.32,
                        "low": 49.00,
                        "avg": 55.10,
                        "high": 61.00,
                        "sellers": 15
                      },
                      "updatedAt": "2026-06-06T03:52:03.597Z"
                    }
                  ]
                }
                """);

        var card = provider.fetchOneCard("Blastoise ex 151");

        assertThat(card.providerCardId()).isEqualTo("517046");
        assertThat(card.cardName()).isEqualTo("Blastoise ex");
        assertThat(card.expansionName()).isEqualTo("SV: Scarlet & Violet 151");
        assertThat(card.cardNumber()).isEqualTo("200/165");
        assertThat(card.displayRawPrice()).isEqualByComparingTo("54.32");
        assertThat(card.rawLowPrice()).isEqualByComparingTo("49.00");
        assertThat(card.rawAveragePrice()).isEqualByComparingTo("55.10");
        assertThat(card.rawHighPrice()).isEqualByComparingTo("61.00");
        assertThat(card.saleCount()).isEqualTo(15);
        assertThat(card.sourceMarket()).isEqualTo("TCGPLAYER");
        assertThat(card.sourceCurrency()).isEqualTo("USD");
        assertThat(capturedRequest.get().url().getQuery()).contains("tcgPlayerId=517046", "includeEbay=true", "days=90");
    }

    @Test
    void textSearchFollowsUpByProviderIdForEbayPsaData() {
        List<ClientRequest> requests = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        PokemonPriceTrackerProvider provider = new PokemonPriceTrackerProvider(
                enabledProperties(),
                WebClient.builder().exchangeFunction(request -> {
                    requests.add(request);
                    String body = calls.getAndIncrement() == 0
                            ? """
                                {
                                  "data": [
                                    {
                                      "id": 517046,
                                      "name": "Blastoise ex - 200/165",
                                      "setName": "SV: Scarlet & Violet 151",
                                      "number": "200/165",
                                      "prices": { "market": 160.40, "low": 114.79, "avg": 160.40, "sellers": 66 }
                                    }
                                  ]
                                }
                                """
                            : """
                                {
                                  "data": [
                                    {
                                      "tcgPlayerId": 517046,
                                      "name": "Blastoise ex",
                                      "setName": "SV: Scarlet & Violet 151",
                                      "cardNumber": "200/165",
                                      "prices": { "market": 160.40, "low": 114.79, "avg": 160.40, "sellers": 66 },
                                      "ebay": {
                                        "psa10": { "avg": 420.00, "salesCount": 5, "currency": "USD" }
                                      }
                                    }
                                  ]
                                }
                                """;
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(body)
                            .build());
                }));

        var card = provider.fetchOneCard("Blastoise ex 151");

        assertThat(card.providerCardId()).isEqualTo("517046");
        assertThat(card.cardName()).isEqualTo("Blastoise ex");
        assertThat(card.psa10Price().displayPrice()).isEqualByComparingTo("420.00");
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).url().getQuery()).contains("search=Blastoise", "includeEbay=true", "days=90");
        assertThat(requests.get(1).url().getQuery()).contains("tcgPlayerId=517046", "includeEbay=true", "days=90");
    }

    @Test
    void planAndQuotaErrorsAreDistinguished() {
        PokemonPriceTrackerProvider forbiddenProvider = providerWithResponse(new AtomicReference<>(), HttpStatus.FORBIDDEN, "{}");
        assertThatThrownBy(() -> forbiddenProvider.fetchOneCard("490294"))
                .isInstanceOf(PokemonPriceTrackerProviderException.class)
                .hasMessageContaining("subscription plan");

        PokemonPriceTrackerProvider quotaProvider = providerWithResponse(new AtomicReference<>(), HttpStatus.TOO_MANY_REQUESTS, "{}");
        assertThatThrownBy(() -> quotaProvider.fetchOneCard("490294"))
                .isInstanceOf(PokemonPriceTrackerProviderException.class)
                .hasMessageContaining("credit limit");
    }

    private PokemonPriceTrackerProvider providerWithResponse(
            AtomicReference<ClientRequest> capturedRequest,
            HttpStatus status,
            String body) {
        return new PokemonPriceTrackerProvider(
                enabledProperties(),
                WebClient.builder().exchangeFunction(request -> {
                    capturedRequest.set(request);
                    return Mono.just(ClientResponse.create(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(body)
                            .build());
                }));
    }

    private PokemonPriceTrackerPricingProperties enabledProperties() {
        PokemonPriceTrackerPricingProperties properties = new PokemonPriceTrackerPricingProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://www.pokemonpricetracker.com/api/v2");
        properties.setApiKey("test-ppt-key");
        return properties;
    }
}
