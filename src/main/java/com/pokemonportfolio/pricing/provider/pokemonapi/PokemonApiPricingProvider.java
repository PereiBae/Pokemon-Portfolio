package com.pokemonportfolio.pricing.provider.pokemonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PokemonApiPricingProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final PokemonApiPricingProperties properties;
    private final WebClient webClient;

    public PokemonApiPricingProvider(
            PokemonApiPricingProperties properties,
            WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    public String providerName() {
        return "PokemonApiPricingProvider";
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public PokemonApiPricingCardView fetchCardById(Long cardId) {
        requireEnabledAndConfigured();
        if (cardId == null) {
            throw new IllegalArgumentException("Card id is required");
        }
        try {
            PokemonApiCardResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/cards/{cardId}").build(cardId))
                    .headers(this::addRapidApiHeaders)
                    .retrieve()
                    .bodyToMono(PokemonApiCardResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (response == null || response.data() == null) {
                throw new PokemonApiPricingProviderException("Pokemon API card response was empty.");
            }
            return toCardView(response.data());
        } catch (PokemonApiPricingProviderException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PokemonApiPricingProviderException("Pokemon API card lookup failed.", ex);
        }
    }

    public Optional<PokemonApiPricingCardView> searchFirstCard(String query) {
        requireEnabledAndConfigured();
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query is required");
        }
        try {
            PokemonApiCardListResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cards")
                            .queryParam("search", query.trim())
                            .queryParam("sort", "relevance")
                            .queryParam("per_page", 1)
                            .queryParam("page", 1)
                            .build())
                    .headers(this::addRapidApiHeaders)
                    .retrieve()
                    .bodyToMono(PokemonApiCardListResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (response == null || response.data() == null || response.data().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(toCardView(response.data().getFirst()));
        } catch (RuntimeException ex) {
            throw new PokemonApiPricingProviderException("Pokemon API card search failed.", ex);
        }
    }

    public PokemonApiPricingCardView fetchOneCard(String searchOrId) {
        if (searchOrId == null || searchOrId.isBlank()) {
            throw new IllegalArgumentException("Search query or card id is required");
        }
        String value = searchOrId.trim();
        if (value.chars().allMatch(Character::isDigit)) {
            return fetchCardById(Long.valueOf(value));
        }
        return searchFirstCard(value)
                .orElseThrow(() -> new PokemonApiPricingProviderException("Pokemon API card search returned no results."));
    }

    public PokemonApiSealedProductPriceView fetchProductById(Long productId) {
        requireEnabledAndConfigured();
        if (productId == null) {
            throw new IllegalArgumentException("Product id is required");
        }
        try {
            PokemonApiProductResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/products/{productId}").build(productId))
                    .headers(this::addRapidApiHeaders)
                    .retrieve()
                    .bodyToMono(PokemonApiProductResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (response == null || response.data() == null) {
                throw new PokemonApiPricingProviderException("Pokemon API product response was empty.");
            }
            return toProductView(response.data());
        } catch (PokemonApiPricingProviderException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PokemonApiPricingProviderException("Pokemon API product lookup failed.", ex);
        }
    }

    private void requireEnabledAndConfigured() {
        if (!properties.isEnabled()) {
            throw new PokemonApiPricingProviderException("Pokemon API pricing provider is disabled.");
        }
        if (properties.getRapidApiKey() == null || properties.getRapidApiKey().isBlank()) {
            throw new PokemonApiPricingProviderException("Pokemon API RapidAPI key is not configured.");
        }
    }

    private void addRapidApiHeaders(org.springframework.http.HttpHeaders headers) {
        headers.set("x-rapidapi-key", properties.getRapidApiKey().trim());
        String host = baseUrlHost();
        if (host != null && !host.isBlank()) {
            headers.set("x-rapidapi-host", host);
        }
    }

    private String baseUrlHost() {
        try {
            return URI.create(properties.getBaseUrl()).getHost();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private PokemonApiPricingCardView toCardView(PokemonApiCard card) {
        PokemonApiPrices prices = card.prices();
        PokemonApiTcgPlayerPrices tcgPlayer = prices == null ? null : prices.tcgPlayer();
        PokemonApiEbayPrices ebay = prices == null ? null : prices.ebay();
        String ebayCurrency = ebay == null ? null : blankToNull(ebay.currency());
        return new PokemonApiPricingCardView(
                card.id(),
                blankToNull(card.name()),
                card.episode() == null ? null : blankToNull(card.episode().name()),
                blankToNull(card.cardNumber()),
                blankToNull(card.image()),
                tcgPlayer == null ? null : blankToNull(tcgPlayer.currency()),
                tcgPlayer == null ? null : tcgPlayer.marketPrice(),
                tcgPlayer == null ? null : tcgPlayer.midPrice(),
                psaPrice(ebay, "8", ebayCurrency),
                psaPrice(ebay, "9", ebayCurrency),
                psaPrice(ebay, "10", ebayCurrency));
    }

    private PokemonApiPsaPriceView psaPrice(PokemonApiEbayPrices ebay, String grade, String currency) {
        if (ebay == null || ebay.graded() == null) {
            return null;
        }
        Map<String, PokemonApiGradedPricePayload> psaPrices = ebay.graded().get("psa");
        if (psaPrices == null) {
            psaPrices = ebay.graded().get("PSA");
        }
        if (psaPrices == null || psaPrices.get(grade) == null) {
            return null;
        }
        PokemonApiGradedPricePayload price = psaPrices.get(grade);
        return new PokemonApiPsaPriceView(
                Integer.valueOf(grade),
                price.medianPrice(),
                price.sampleSize(),
                currency);
    }

    private PokemonApiSealedProductPriceView toProductView(PokemonApiProduct product) {
        PokemonApiProductPrices prices = product.prices();
        PokemonApiCardmarketProductPrices cardmarket = prices == null ? null : prices.cardmarket();
        PokemonApiTcgPlayerPrices tcgPlayer = prices == null ? null : prices.tcgPlayer();
        return new PokemonApiSealedProductPriceView(
                product.id(),
                blankToNull(product.name()),
                product.episode() == null ? null : blankToNull(product.episode().name()),
                blankToNull(product.image()),
                cardmarket == null ? null : blankToNull(cardmarket.currency()),
                cardmarket == null ? null : cardmarket.lowest(),
                cardmarket == null ? null : cardmarket.lowestDe(),
                cardmarket == null ? null : cardmarket.lowestFr(),
                cardmarket == null ? null : cardmarket.lowestEs(),
                cardmarket == null ? null : cardmarket.lowestIt(),
                tcgPlayer == null ? null : blankToNull(tcgPlayer.currency()),
                tcgPlayer == null ? null : tcgPlayer.marketPrice(),
                tcgPlayer == null ? null : tcgPlayer.midPrice());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiCardResponse(PokemonApiCard data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiCardListResponse(List<PokemonApiCard> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiProductResponse(PokemonApiProduct data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiCard(
            Long id,
            String name,
            @JsonProperty("card_number") String cardNumber,
            String image,
            PokemonApiEpisode episode,
            PokemonApiPrices prices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiProduct(
            Long id,
            String name,
            String image,
            PokemonApiEpisode episode,
            PokemonApiProductPrices prices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiEpisode(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiPrices(
            @JsonProperty("tcg_player") PokemonApiTcgPlayerPrices tcgPlayer,
            PokemonApiEbayPrices ebay) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiProductPrices(
            @JsonProperty("tcg_player") PokemonApiTcgPlayerPrices tcgPlayer,
            PokemonApiCardmarketProductPrices cardmarket) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiTcgPlayerPrices(
            String currency,
            @JsonProperty("market_price") BigDecimal marketPrice,
            @JsonProperty("mid_price") BigDecimal midPrice) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiEbayPrices(
            String currency,
            Map<String, Map<String, PokemonApiGradedPricePayload>> graded) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiGradedPricePayload(
            @JsonProperty("median_price") BigDecimal medianPrice,
            @JsonProperty("sample_size") Integer sampleSize) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonApiCardmarketProductPrices(
            String currency,
            BigDecimal lowest,
            @JsonProperty("lowest_DE") BigDecimal lowestDe,
            @JsonProperty("lowest_FR") BigDecimal lowestFr,
            @JsonProperty("lowest_ES") BigDecimal lowestEs,
            @JsonProperty("lowest_IT") BigDecimal lowestIt) {}
}
