package com.pokemonportfolio.pricing.provider.pokemonapi;

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

class PokemonApiPricingProviderTest {

    @Test
    void providerIsDisabledByDefaultAndDoesNotCallApi() {
        PokemonApiPricingProperties properties = new PokemonApiPricingProperties();
        AtomicInteger calls = new AtomicInteger();
        PokemonApiPricingProvider provider = new PokemonApiPricingProvider(
                properties,
                WebClient.builder().exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.empty();
                }));

        assertThat(provider.isEnabled()).isFalse();
        assertThatThrownBy(() -> provider.fetchCardById(3852L))
                .isInstanceOf(PokemonApiPricingProviderException.class)
                .hasMessageContaining("disabled");
        assertThat(calls).hasValue(0);
    }

    @Test
    void enabledProviderRequiresRapidApiKeyBeforeCallingApi() {
        PokemonApiPricingProperties properties = enabledProperties();
        properties.setRapidApiKey("");
        AtomicInteger calls = new AtomicInteger();
        PokemonApiPricingProvider provider = new PokemonApiPricingProvider(
                properties,
                WebClient.builder().exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return Mono.empty();
                }));

        assertThatThrownBy(() -> provider.fetchCardById(3852L))
                .isInstanceOf(PokemonApiPricingProviderException.class)
                .hasMessageContaining("RapidAPI key");
        assertThat(calls).hasValue(0);
    }

    @Test
    void fetchCardByIdMapsRawTcgPlayerAndPsaPrices() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        PokemonApiPricingProvider provider = providerWithResponse(capturedRequest, """
                {
                  "data": {
                    "id": 3852,
                    "name": "Giratina VSTAR",
                    "card_number": "GG69",
                    "image": "https://images.tcggo.example/giratina.png",
                    "episode": { "name": "Crown Zenith" },
                    "prices": {
                      "tcg_player": {
                        "currency": "USD",
                        "market_price": 146.69,
                        "mid_price": 163.71
                      },
                      "ebay": {
                        "currency": "USD",
                        "graded": {
                          "psa": {
                            "8": { "median_price": 600.00, "sample_size": 3 },
                            "9": { "median_price": 1200.00, "sample_size": 4 },
                            "10": { "median_price": 2941.00, "sample_size": 5 }
                          }
                        }
                      }
                    }
                  }
                }
                """);

        PokemonApiPricingCardView card = provider.fetchCardById(3852L);

        assertThat(card.cardId()).isEqualTo(3852L);
        assertThat(card.cardName()).isEqualTo("Giratina VSTAR");
        assertThat(card.expansionName()).isEqualTo("Crown Zenith");
        assertThat(card.cardNumber()).isEqualTo("GG69");
        assertThat(card.imageUrl()).isEqualTo("https://images.tcggo.example/giratina.png");
        assertThat(card.tcgPlayerCurrency()).isEqualTo("USD");
        assertThat(card.tcgPlayerMarketPrice()).isEqualByComparingTo("146.69");
        assertThat(card.tcgPlayerMidPrice()).isEqualByComparingTo("163.71");
        assertThat(card.psa8Price().medianPrice()).isEqualByComparingTo("600.00");
        assertThat(card.psa8Price().sampleSize()).isEqualTo(3);
        assertThat(card.psa9Price().medianPrice()).isEqualByComparingTo("1200.00");
        assertThat(card.psa10Price().medianPrice()).isEqualByComparingTo("2941.00");
        assertThat(capturedRequest.get().url().getPath()).isEqualTo("/cards/3852");
        assertThat(capturedRequest.get().headers().getFirst("x-rapidapi-key")).isEqualTo("test-rapidapi-key");
        assertThat(capturedRequest.get().headers().getFirst("x-rapidapi-host"))
                .isEqualTo("pokemon-tcg-api.p.rapidapi.com");
    }

    @Test
    void fetchOneCardUsesSearchWhenInputIsNotNumeric() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        PokemonApiPricingProvider provider = providerWithResponse(capturedRequest, """
                {
                  "data": [
                    {
                      "id": 100,
                      "name": "Scizor ex",
                      "card_number": "205",
                      "image": "https://images.tcggo.example/scizor.png",
                      "episode": { "name": "Obsidian Flames" },
                      "prices": {
                        "tcg_player": {
                          "currency": "USD",
                          "market_price": 45.12,
                          "mid_price": 51.00
                        }
                      }
                    }
                  ]
                }
                """);

        PokemonApiPricingCardView card = provider.fetchOneCard("Scizor Obsidian Flames 205");

        assertThat(card.cardName()).isEqualTo("Scizor ex");
        assertThat(card.tcgPlayerMarketPrice()).isEqualByComparingTo("45.12");
        assertThat(capturedRequest.get().url().getPath()).isEqualTo("/cards");
        assertThat(capturedRequest.get().url().getQuery())
                .contains("search=Scizor", "Obsidian", "Flames", "205", "per_page=1", "page=1");
    }

    @Test
    void fetchProductByIdMapsSealedProductPricesWhenPresent() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        PokemonApiPricingProvider provider = providerWithResponse(capturedRequest, """
                {
                  "data": {
                    "id": 20039,
                    "name": "Crown Zenith Elite Trainer Box",
                    "image": "https://images.tcggo.example/crown-zenith-etb.png",
                    "episode": { "name": "Crown Zenith" },
                    "prices": {
                      "cardmarket": {
                        "currency": "EUR",
                        "lowest": 89.99,
                        "lowest_DE": 85.00,
                        "lowest_FR": 92.50,
                        "lowest_ES": 88.00,
                        "lowest_IT": 90.00
                      },
                      "tcg_player": {
                        "currency": "USD",
                        "market_price": 105.25,
                        "mid_price": 112.00
                      }
                    }
                  }
                }
                """);

        PokemonApiSealedProductPriceView product = provider.fetchProductById(20039L);

        assertThat(product.productId()).isEqualTo(20039L);
        assertThat(product.productName()).isEqualTo("Crown Zenith Elite Trainer Box");
        assertThat(product.expansionName()).isEqualTo("Crown Zenith");
        assertThat(product.cardmarketCurrency()).isEqualTo("EUR");
        assertThat(product.cardmarketLowestPrice()).isEqualByComparingTo("89.99");
        assertThat(product.cardmarketLowestDePrice()).isEqualByComparingTo("85.00");
        assertThat(product.tcgPlayerCurrency()).isEqualTo("USD");
        assertThat(product.tcgPlayerMarketPrice()).isEqualByComparingTo("105.25");
        assertThat(capturedRequest.get().url().getPath()).isEqualTo("/products/20039");
    }

    private PokemonApiPricingProvider providerWithResponse(
            AtomicReference<ClientRequest> capturedRequest,
            String body) {
        return new PokemonApiPricingProvider(
                enabledProperties(),
                WebClient.builder().exchangeFunction(request -> {
                    capturedRequest.set(request);
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(body)
                            .build());
                }));
    }

    private PokemonApiPricingProperties enabledProperties() {
        PokemonApiPricingProperties properties = new PokemonApiPricingProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://pokemon-tcg-api.p.rapidapi.com");
        properties.setRapidApiKey("test-rapidapi-key");
        return properties;
    }
}
