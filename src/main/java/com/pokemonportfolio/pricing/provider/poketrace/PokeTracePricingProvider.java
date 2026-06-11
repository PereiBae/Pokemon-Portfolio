package com.pokemonportfolio.pricing.provider.poketrace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.provider.ExternalPricingGradedPriceView;
import com.pokemonportfolio.pricing.provider.ExternalPricingProbeCardView;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PokeTracePricingProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final PokeTracePricingProperties properties;
    private final WebClient webClient;

    public PokeTracePricingProvider(
            PokeTracePricingProperties properties,
            WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    public String providerName() {
        return "PokeTracePricingProvider";
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public ExternalPricingProbeCardView fetchOneCard(String searchOrId) {
        requireEnabledAndConfigured();
        String value = requireSearchOrId(searchOrId);
        if (looksLikeProviderId(value)) {
            return fetchCardById(value);
        }
        return searchFirstCard(value)
                .orElseThrow(() -> new PokeTracePricingProviderException("PokeTrace search returned no results."));
    }

    public ExternalPricingProbeCardView fetchCardById(String cardId) {
        requireEnabledAndConfigured();
        String requiredCardId = requireSearchOrId(cardId);
        try {
            PokeTraceCardResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/cards/{cardId}").build(requiredCardId))
                    .headers(headers -> headers.set("X-API-Key", properties.getApiKey().trim()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new PokeTracePricingProviderException(errorMessage(
                                    clientResponse.statusCode(),
                                    body))))
                    .bodyToMono(PokeTraceCardResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (response == null || response.data() == null) {
                throw new PokeTracePricingProviderException("PokeTrace card response was empty.");
            }
            return toView(response.data(), sourceUrl("/cards/" + requiredCardId), "Provider card id lookup");
        } catch (PokeTracePricingProviderException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            throw new PokeTracePricingProviderException(errorMessage(ex.getStatusCode(), ex.getResponseBodyAsString()), ex);
        } catch (RuntimeException ex) {
            throw new PokeTracePricingProviderException(
                    "PokeTrace card lookup failed. " + safeRuntimeMessage(ex),
                    ex);
        }
    }

    public Optional<ExternalPricingProbeCardView> searchFirstCard(String query) {
        requireEnabledAndConfigured();
        String requiredQuery = requireSearchOrId(query);
        try {
            PokeTraceCardListResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cards")
                            .queryParam("search", requiredQuery)
                            .queryParam("market", "US")
                            .queryParam("limit", 1)
                            .build())
                    .headers(headers -> headers.set("X-API-Key", properties.getApiKey().trim()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new PokeTracePricingProviderException(errorMessage(
                                    clientResponse.statusCode(),
                                    body))))
                    .bodyToMono(PokeTraceCardListResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (response == null || response.data() == null || response.data().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(toView(response.data().getFirst(), sourceUrl("/cards?search=" + requiredQuery),
                    "Search result; verify exact identity before production use"));
        } catch (PokeTracePricingProviderException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            throw new PokeTracePricingProviderException(errorMessage(ex.getStatusCode(), ex.getResponseBodyAsString()), ex);
        } catch (RuntimeException ex) {
            throw new PokeTracePricingProviderException(
                    "PokeTrace card search failed. " + safeRuntimeMessage(ex),
                    ex);
        }
    }

    private ExternalPricingProbeCardView toView(
            PokeTraceCard card,
            String sourceUrl,
            String matchQuality) {
        RawCandidate rawCandidate = rawCandidate(card);
        ExternalPricingGradedPriceView psa8 = gradedPrice(card, "PSA_8");
        ExternalPricingGradedPriceView psa9 = gradedPrice(card, "PSA_9");
        ExternalPricingGradedPriceView psa10 = gradedPrice(card, "PSA_10");
        return new ExternalPricingProbeCardView(
                blankToNull(card.id()),
                blankToNull(card.name()),
                card.set() == null ? null : blankToNull(card.set().name()),
                blankToNull(card.cardNumber()),
                blankToNull(card.market()),
                blankToNull(card.variant()),
                blankToNull(card.image()),
                rawCandidate.sourceMarket(),
                rawCandidate.currency(),
                rawCandidate.averagePrice(),
                rawCandidate.lowPrice(),
                rawCandidate.averagePrice(),
                rawCandidate.highPrice(),
                rawCandidate.saleCount(),
                parseTimestamp(card.lastUpdated()),
                psa8,
                psa9,
                psa10,
                gradedStatus(card, psa8, psa9, psa10),
                null,
                matchQuality,
                confidence(rawCandidate),
                sourceUrl);
    }

    private RawCandidate rawCandidate(PokeTraceCard card) {
        Map<String, Map<String, PokeTraceTierPrice>> prices = card.prices();
        RawCandidate candidate = firstTier(prices, "tcgplayer", "NEAR_MINT", card.currency());
        if (candidate.hasAverage()) {
            return candidate;
        }
        candidate = firstTier(prices, "ebay", "NEAR_MINT", card.currency());
        if (candidate.hasAverage()) {
            return candidate;
        }
        candidate = firstTier(prices, "cardmarket_unsold", "NEAR_MINT", card.currency());
        if (candidate.hasAverage()) {
            return candidate;
        }
        candidate = firstTier(prices, "cardmarket", "AGGREGATED", card.currency());
        if (candidate.hasAverage()) {
            return candidate;
        }
        return RawCandidate.empty();
    }

    private RawCandidate firstTier(
            Map<String, Map<String, PokeTraceTierPrice>> prices,
            String sourceMarket,
            String tier,
            String defaultCurrency) {
        if (prices == null || prices.get(sourceMarket) == null) {
            return RawCandidate.empty();
        }
        PokeTraceTierPrice price = prices.get(sourceMarket).get(tier);
        if (price == null) {
            price = prices.get(sourceMarket).get(tier.toLowerCase(Locale.ROOT));
        }
        if (price == null) {
            return RawCandidate.empty();
        }
        return new RawCandidate(
                sourceMarket.toUpperCase(Locale.ROOT),
                blankToNull(defaultCurrency),
                price.avg(),
                price.low(),
                price.high(),
                price.saleCount());
    }

    private ExternalPricingGradedPriceView gradedPrice(PokeTraceCard card, String gradeKey) {
        Map<String, Map<String, PokeTraceTierPrice>> prices = card.prices();
        PokeTraceTierPrice price = tier(prices, "ebay", gradeKey)
                .or(() -> tier(prices, "cardmarket_unsold", gradeKey))
                .orElse(null);
        if (price == null) {
            return null;
        }
        return new ExternalPricingGradedPriceView(
                gradeKey.replace('_', ' '),
                price.avg(),
                null,
                null,
                price.saleCount(),
                blankToNull(card.currency()));
    }

    private Optional<PokeTraceTierPrice> tier(
            Map<String, Map<String, PokeTraceTierPrice>> prices,
            String market,
            String key) {
        if (prices == null || prices.get(market) == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(prices.get(market).get(key));
    }

    private String gradedStatus(
            PokeTraceCard card,
            ExternalPricingGradedPriceView psa8,
            ExternalPricingGradedPriceView psa9,
            ExternalPricingGradedPriceView psa10) {
        if (psa8 != null || psa9 != null || psa10 != null) {
            return "Graded data returned successfully.";
        }
        if (Boolean.FALSE.equals(card.hasGraded())) {
            return "Graded data absent for this card.";
        }
        if (card.gradedOptions() == null || card.gradedOptions().isEmpty()) {
            return "Graded data unavailable in this response; verify whether the current plan exposes it.";
        }
        return "Graded options returned, but PSA 8/9/10 prices were not present.";
    }

    private ConfidenceRating confidence(RawCandidate candidate) {
        if (!candidate.hasAverage()) {
            return ConfidenceRating.LOW;
        }
        if (candidate.saleCount() != null && candidate.saleCount() >= 10) {
            return ConfidenceRating.MEDIUM;
        }
        return ConfidenceRating.LOW;
    }

    private void requireEnabledAndConfigured() {
        if (!properties.isEnabled()) {
            throw new PokeTracePricingProviderException("PokeTrace provider is disabled.");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new PokeTracePricingProviderException("PokeTrace API key is not configured.");
        }
    }

    private String requireSearchOrId(String searchOrId) {
        if (searchOrId == null || searchOrId.isBlank()) {
            throw new IllegalArgumentException("Card id or search text is required.");
        }
        return searchOrId.trim();
    }

    private boolean looksLikeProviderId(String value) {
        return value.matches("[0-9a-fA-F-]{20,}") || value.startsWith("eu_");
    }

    private String sourceUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return path;
        }
        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) + path
                : baseUrl + path;
    }

    private String errorMessage(HttpStatusCode statusCode, String body) {
        int status = statusCode.value();
        if (status == 401 || status == 403) {
            return "PokeTrace authentication or subscription plan blocked this request.";
        }
        if (status == 404) {
            return "PokeTrace card was not found.";
        }
        if (status == 429) {
            return "PokeTrace quota or rate limit was exceeded.";
        }
        if (status >= 500) {
            return "PokeTrace upstream service failed.";
        }
        return body == null || body.isBlank()
                ? "PokeTrace request failed with status " + status + "."
                : "PokeTrace request failed with status " + status + ": " + sanitizeBody(body);
    }

    private String safeRuntimeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "No upstream detail was provided.";
        }
        return sanitizeBody(message);
    }

    private String sanitizeBody(String body) {
        String sanitized = body
                .replaceAll("(?i)(x-api-key[=: ]+)[^,\\s}]+", "$1[hidden]")
                .replaceAll("(?i)(authorization[=: ]+bearer )[A-Za-z0-9._~-]+", "$1[hidden]")
                .replaceAll("\\s+", " ")
                .trim();
        return sanitized.length() > 280 ? sanitized.substring(0, 280) + "..." : sanitized;
    }

    private OffsetDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RawCandidate(
            String sourceMarket,
            String currency,
            BigDecimal averagePrice,
            BigDecimal lowPrice,
            BigDecimal highPrice,
            Integer saleCount) {

        static RawCandidate empty() {
            return new RawCandidate(null, null, null, null, null, null);
        }

        boolean hasAverage() {
            return averagePrice != null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokeTraceCardResponse(PokeTraceCard data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokeTraceCardListResponse(List<PokeTraceCard> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokeTraceCard(
            String id,
            String name,
            String image,
            String cardNumber,
            PokeTraceSet set,
            String variant,
            String rarity,
            String market,
            String currency,
            Map<String, Map<String, PokeTraceTierPrice>> prices,
            List<String> gradedOptions,
            List<String> conditionOptions,
            Integer totalSaleCount,
            Boolean hasGraded,
            String lastUpdated) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokeTraceSet(String slug, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokeTraceTierPrice(
            BigDecimal avg,
            BigDecimal low,
            BigDecimal high,
            Integer saleCount,
            Boolean approxSaleCount,
            @JsonProperty("avg1d") BigDecimal average1d,
            @JsonProperty("avg7d") BigDecimal average7d,
            @JsonProperty("avg30d") BigDecimal average30d) {}
}
