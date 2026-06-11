package com.pokemonportfolio.pricing.provider.poketrace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

class PokeTracePricingProviderTest {

    @Test
    void disabledProviderDoesNotCallApi() {
        PokeTracePricingProperties properties = new PokeTracePricingProperties();
        AtomicInteger calls = new AtomicInteger();
        PokeTracePricingProvider provider = new PokeTracePricingProvider(
                properties,
                WebClient.builder().exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.empty();
                }));

        assertThat(provider.isEnabled()).isFalse();
        assertThatThrownBy(() -> provider.fetchOneCard("019bff77-befa-771d-bab0-f5909f0a78c9"))
                .isInstanceOf(PokeTracePricingProviderException.class)
                .hasMessageContaining("disabled");
        assertThat(calls).hasValue(0);
    }

    @Test
    void enabledProviderRequiresApiKeyBeforeCallingApi() {
        PokeTracePricingProperties properties = enabledProperties();
        properties.setApiKey("");
        AtomicInteger calls = new AtomicInteger();
        PokeTracePricingProvider provider = new PokeTracePricingProvider(
                properties,
                WebClient.builder().exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.empty();
                }));

        assertThatThrownBy(() -> provider.fetchOneCard("019bff77-befa-771d-bab0-f5909f0a78c9"))
                .isInstanceOf(PokeTracePricingProviderException.class)
                .hasMessageContaining("API key");
        assertThat(calls).hasValue(0);
    }

    @Test
    void fetchCardByIdSendsApiKeyHeaderAndMapsNearMintVariantAndPsaPrices() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        PokeTracePricingProvider provider = providerWithResponse(capturedRequest, HttpStatus.OK, """
                {
                  "data": {
                    "id": "019bff77-befa-771d-bab0-f5909f0a78c9",
                    "name": "Charizard ex",
                    "image": "https://images.example/charizard.png",
                    "cardNumber": "199",
                    "variant": "Holofoil",
                    "market": "US",
                    "currency": "USD",
                    "set": { "slug": "sv3pt5", "name": "151" },
                    "lastUpdated": "2026-06-08T02:00:00Z",
                    "prices": {
                      "tcgplayer": {
                        "NEAR_MINT": { "avg": 118.45, "low": 111.00, "high": 130.00, "saleCount": 14 }
                      },
                      "ebay": {
                        "PSA_8": { "avg": 175.00, "saleCount": 3 },
                        "PSA_9": { "avg": 260.00, "saleCount": 4 },
                        "PSA_10": { "avg": 640.00, "saleCount": 8 }
                      }
                    },
                    "gradedOptions": ["PSA_8", "PSA_9", "PSA_10"],
                    "conditionOptions": ["NEAR_MINT"],
                    "hasGraded": true
                  }
                }
                """);

        var card = provider.fetchOneCard("019bff77-befa-771d-bab0-f5909f0a78c9");

        assertThat(card.providerCardId()).isEqualTo("019bff77-befa-771d-bab0-f5909f0a78c9");
        assertThat(card.cardName()).isEqualTo("Charizard ex");
        assertThat(card.expansionName()).isEqualTo("151");
        assertThat(card.cardNumber()).isEqualTo("199");
        assertThat(card.printVariant()).isEqualTo("Holofoil");
        assertThat(card.rawNearMintPrice()).isEqualByComparingTo("118.45");
        assertThat(card.rawLowPrice()).isEqualByComparingTo("111.00");
        assertThat(card.rawHighPrice()).isEqualByComparingTo("130.00");
        assertThat(card.saleCount()).isEqualTo(14);
        assertThat(card.sourceMarket()).isEqualTo("TCGPLAYER");
        assertThat(card.sourceCurrency()).isEqualTo("USD");
        assertThat(card.psa8Price().displayPrice()).isEqualByComparingTo("175.00");
        assertThat(card.psa9Price().displayPrice()).isEqualByComparingTo("260.00");
        assertThat(card.psa10Price().displayPrice()).isEqualByComparingTo("640.00");
        assertThat(card.gradedDataStatus()).isEqualTo("Graded data returned successfully.");
        assertThat(capturedRequest.get().url().getPath()).contains("/cards/019bff77-befa-771d-bab0-f5909f0a78c9");
        assertThat(capturedRequest.get().headers().getFirst("X-API-Key")).isEqualTo("test-poketrace-key");
    }

    @Test
    void quotaAndSubscriptionErrorsAreFriendly() {
        PokeTracePricingProvider quotaProvider = providerWithResponse(new AtomicReference<>(), HttpStatus.TOO_MANY_REQUESTS, "{}");
        assertThatThrownBy(() -> quotaProvider.fetchOneCard("019bff77-befa-771d-bab0-f5909f0a78c9"))
                .isInstanceOf(PokeTracePricingProviderException.class)
                .hasMessageContaining("quota");

        PokeTracePricingProvider forbiddenProvider = providerWithResponse(new AtomicReference<>(), HttpStatus.FORBIDDEN, "{}");
        assertThatThrownBy(() -> forbiddenProvider.fetchOneCard("019bff77-befa-771d-bab0-f5909f0a78c9"))
                .isInstanceOf(PokeTracePricingProviderException.class)
                .hasMessageContaining("subscription");
    }

    @Test
    void nonPlanHttpErrorsIncludeSafeStatusAndBodyDetail() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        PokeTracePricingProvider badRequestProvider = providerWithResponse(capturedRequest, HttpStatus.BAD_REQUEST, """
                {"error":"invalid market parameter"}
                """);

        assertThatThrownBy(() -> badRequestProvider.fetchOneCard("Scizor"))
                .isInstanceOf(PokeTracePricingProviderException.class)
                .hasMessageContaining("status 400")
                .hasMessageContaining("invalid market parameter")
                .hasMessageNotContaining("test-poketrace-key");
        assertThat(capturedRequest.get().url().getQuery()).contains("search=Scizor", "market=US");
    }

    private PokeTracePricingProvider providerWithResponse(
            AtomicReference<ClientRequest> capturedRequest,
            HttpStatus status,
            String body) {
        return new PokeTracePricingProvider(
                enabledProperties(),
                WebClient.builder().exchangeFunction(request -> {
                    capturedRequest.set(request);
                    return Mono.just(ClientResponse.create(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(body)
                            .build());
                }));
    }

    private PokeTracePricingProperties enabledProperties() {
        PokeTracePricingProperties properties = new PokeTracePricingProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://api.poketrace.com/v1");
        properties.setApiKey("test-poketrace-key");
        return properties;
    }
}
