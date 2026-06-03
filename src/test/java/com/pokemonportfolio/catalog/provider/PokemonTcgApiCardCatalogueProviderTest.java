package com.pokemonportfolio.catalog.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.catalog.service.OfficialCardSearchPage;
import com.pokemonportfolio.catalog.service.OfficialCardSearchRequest;
import com.pokemonportfolio.catalog.service.OfficialCardSearchResult;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.LanguageMarket;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class PokemonTcgApiCardCatalogueProviderTest {

    @Test
    void mapsSearchResponseToOfficialCardDto() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        PokemonTcgApiProperties properties = new PokemonTcgApiProperties();
        properties.setBaseUrl("https://api.example.test/v2");
        properties.setApiKey("test-api-key");

        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(request -> {
            capturedRequest.set(request);
            String body = """
                    {
                      "data": [
                        {
                          "id": "sv4pt5-91",
                          "name": "Pikachu",
                          "number": "91",
                          "rarity": "Illustration Rare",
                          "set": {
                            "id": "sv4pt5",
                            "name": "Paldean Fates",
                            "series": "Scarlet & Violet",
                            "releaseDate": "2024/01/26"
                          },
                          "images": {
                            "small": "https://images.example/pikachu-small.png",
                            "large": "https://images.example/pikachu-large.png"
                          },
                          "tcgplayer": {
                            "url": "https://prices.example/pikachu",
                            "prices": {
                              "normal": { "market": 12.00 },
                              "holofoil": { "market": 15.00 },
                              "reverseHolofoil": { "market": 18.00 },
                              "1stEditionHolofoil": { "market": 120.00 }
                            }
                          }
                        }
                      ],
                      "page": 2,
                      "pageSize": 50,
                      "count": 1,
                      "totalCount": 126
                    }
                    """;
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        });

        PokemonTcgApiCardCatalogueProvider provider = new PokemonTcgApiCardCatalogueProvider(properties, webClientBuilder);

        OfficialCardSearchRequest request = new OfficialCardSearchRequest(
                "Pikachu",
                "Pikachu",
                "Paldean Fates",
                "91",
                "Illustration Rare",
                2,
                50);

        OfficialCardSearchPage page = provider.search(request);

        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getPageSize()).isEqualTo(50);
        assertThat(page.getTotalCount()).isEqualTo(126);
        List<OfficialCardSearchResult> results = page.getResults();
        assertThat(results).hasSize(1);
        OfficialCardSearchResult result = results.getFirst();
        assertThat(result.getExternalCardId()).isEqualTo("sv4pt5-91");
        assertThat(result.getName()).isEqualTo("Pikachu");
        assertThat(result.getExternalSetId()).isEqualTo("sv4pt5");
        assertThat(result.getSetName()).isEqualTo("Paldean Fates");
        assertThat(result.getSetSeries()).isEqualTo("Scarlet & Violet");
        assertThat(result.getSetReleaseDate()).isEqualTo(LocalDate.of(2024, 1, 26));
        assertThat(result.getCardNumber()).isEqualTo("91");
        assertThat(result.getRarity()).isEqualTo("Illustration Rare");
        assertThat(result.getImageSmallUrl()).isEqualTo("https://images.example/pikachu-small.png");
        assertThat(result.getImageLargeUrl()).isEqualTo("https://images.example/pikachu-large.png");
        assertThat(result.getExternalCardUrl()).isEqualTo("https://prices.example/pikachu");
        assertThat(result.getAvailableVariants()).containsExactly(
                CardVariant.STANDARD,
                CardVariant.HOLO,
                CardVariant.REVERSE_HOLO,
                CardVariant.FIRST_EDITION_HOLO);
        assertThat(result.getLanguageMarket()).isEqualTo(LanguageMarket.ENGLISH);
        assertThat(result.getSource()).isEqualTo(CatalogSource.POKEMON_TCG_API);
        assertThat(capturedRequest.get().headers().getFirst("X-Api-Key")).isEqualTo("test-api-key");
        assertThat(capturedRequest.get().url().getQuery()).contains("page=2", "pageSize=50");
        assertThat(capturedRequest.get().url().getQuery())
                .contains("name", "set.name", "number", "rarity");
    }
}
