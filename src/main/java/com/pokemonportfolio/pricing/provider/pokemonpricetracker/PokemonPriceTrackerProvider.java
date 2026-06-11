package com.pokemonportfolio.pricing.provider.pokemonpricetracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.provider.ExternalPricingGradedPriceView;
import com.pokemonportfolio.pricing.provider.ExternalPricingProbeCardView;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Optional;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PokemonPriceTrackerProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final PokemonPriceTrackerPricingProperties properties;
    private final WebClient webClient;

    public PokemonPriceTrackerProvider(
            PokemonPriceTrackerPricingProperties properties,
            WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    public String providerName() {
        return "PokemonPriceTrackerProvider";
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public ExternalPricingProbeCardView fetchOneCard(String searchOrId) {
        requireEnabledAndConfigured();
        String value = requireSearchOrId(searchOrId);
        try {
            boolean idLookup = value.chars().allMatch(Character::isDigit);
            JsonNode response = requestCards(value, idLookup);
            JsonNode card = firstCard(response);
            if (card == null || card.isMissingNode() || card.isNull()) {
                throw new PokemonPriceTrackerProviderException("PokemonPriceTracker search returned no results.");
            }
            if (!idLookup && !hasPsaPrice(card)) {
                card = fetchDetailedCardWhenAvailable(card).orElse(card);
            }
            return toView(card, sourceUrl("/cards"), idLookup
                    ? "TCGPlayer/provider id lookup"
                    : "Search result; verify exact identity before production use");
        } catch (PokemonPriceTrackerProviderException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PokemonPriceTrackerProviderException("PokemonPriceTracker card lookup failed.", ex);
        }
    }

    private JsonNode requestCards(String value, boolean idLookup) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/cards")
                            .queryParam("includeBoth", true)
                            .queryParam("includeEbay", true)
                            .queryParam("days", 90)
                            .queryParam("limit", 1);
                    if (idLookup) {
                        builder.queryParam("tcgPlayerId", value);
                    } else {
                        builder.queryParam("search", value);
                    }
                    return builder.build();
                })
                .headers(headers -> headers.setBearerAuth(properties.getApiKey().trim()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new PokemonPriceTrackerProviderException(errorMessage(
                                clientResponse.statusCode(),
                                body))))
                .bodyToMono(JsonNode.class)
                .block(REQUEST_TIMEOUT);
    }

    private Optional<JsonNode> fetchDetailedCardWhenAvailable(JsonNode searchResultCard) {
        String providerId = text(searchResultCard, "tcgPlayerId", "id", "cardId");
        if (providerId == null || providerId.chars().anyMatch(ch -> !Character.isDigit(ch))) {
            return Optional.empty();
        }
        JsonNode detailResponse = requestCards(providerId, true);
        JsonNode detailCard = firstCard(detailResponse);
        if (detailCard == null || detailCard.isMissingNode() || detailCard.isNull()) {
            return Optional.empty();
        }
        return Optional.of(detailCard);
    }

    private ExternalPricingProbeCardView toView(JsonNode card, String sourceUrl, String matchQuality) {
        BigDecimal marketPrice = decimal(card,
                "marketPrice",
                "market_price",
                "price",
                "rawPrice",
                "prices.market",
                "prices.marketPrice",
                "prices.tcgplayer.market",
                "prices.tcgPlayer.market",
                "prices.tcgplayer.marketPrice",
                "prices.tcgPlayer.marketPrice");
        BigDecimal lowPrice = decimal(card,
                "lowPrice",
                "low_price",
                "prices.low",
                "prices.lowPrice",
                "prices.tcgplayer.low",
                "prices.tcgPlayer.low",
                "prices.tcgplayer.lowPrice",
                "prices.tcgPlayer.lowPrice");
        BigDecimal averagePrice = decimal(card,
                "averagePrice",
                "avgPrice",
                "avg_price",
                "prices.average",
                "prices.avg",
                "prices.averagePrice",
                "prices.tcgplayer.average",
                "prices.tcgPlayer.average",
                "prices.tcgplayer.avg",
                "prices.tcgPlayer.avg");
        BigDecimal displayRaw = marketPrice == null ? averagePrice : marketPrice;
        String currency = sourceCurrency(card, displayRaw);
        ExternalPricingGradedPriceView psa8 = gradedPrice(card, "8");
        ExternalPricingGradedPriceView psa9 = gradedPrice(card, "9");
        ExternalPricingGradedPriceView psa10 = gradedPrice(card, "10");
        Integer sellers = integer(card, "sellers", "salesCount", "sampleSize", "prices.sellers", "prices.salesCount");
        return new ExternalPricingProbeCardView(
                text(card, "tcgPlayerId", "id", "cardId"),
                cleanCardName(text(card, "name", "cardName"), text(card, "cardNumber", "number")),
                text(card, "setName", "set.name", "set"),
                text(card, "cardNumber", "number"),
                text(card, "language", "market"),
                text(card, "printing", "variant", "printVariant"),
                text(card, "image", "imageUrl", "images.small", "images.large"),
                text(card, "sourceMarket", "marketplace", "priceSource") == null ? "TCGPLAYER" : text(card, "sourceMarket", "marketplace", "priceSource"),
                currency,
                displayRaw,
                lowPrice,
                averagePrice == null ? displayRaw : averagePrice,
                decimal(card, "highPrice", "high_price", "prices.high", "prices.highPrice"),
                sellers,
                parseTimestamp(text(card, "lastPriceUpdate", "updatedAt", "lastUpdated")),
                psa8,
                psa9,
                psa10,
                gradedStatus(psa8, psa9, psa10, card),
                planOrQuotaLimitation(card),
                matchQuality,
                confidence(displayRaw, sellers),
                sourceUrl);
    }

    private String sourceCurrency(JsonNode card, BigDecimal displayRaw) {
        String currency = text(card,
                "currency",
                "sourceCurrency",
                "priceCurrency",
                "prices.currency",
                "prices.tcgplayer.currency",
                "prices.tcgPlayer.currency");
        if (currency == null && displayRaw != null) {
            return "USD";
        }
        return currency;
    }

    private String cleanCardName(String name, String cardNumber) {
        if (name == null || cardNumber == null || cardNumber.isBlank()) {
            return name;
        }
        String suffix = " - " + cardNumber;
        return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
    }

    private JsonNode firstCard(JsonNode response) {
        if (response == null || response.isNull()) {
            return null;
        }
        JsonNode data = response.path("data");
        if (data.isArray()) {
            return data.isEmpty() ? null : data.get(0);
        }
        if (data.isObject()) {
            return data;
        }
        if (response.isArray()) {
            return response.isEmpty() ? null : response.get(0);
        }
        return response.isObject() ? response : null;
    }

    private ExternalPricingGradedPriceView gradedPrice(JsonNode card, String grade) {
        JsonNode gradeNode = gradeNode(card, grade);
        BigDecimal directPrice = decimal(card,
                "psa" + grade + "Price",
                "psa_" + grade,
                "PSA_" + grade);
        if (gradeNode == null && directPrice == null) {
            return null;
        }
        BigDecimal average = gradeNode == null ? directPrice : decimal(gradeNode, "averagePrice", "avg", "avgPrice");
        BigDecimal median = gradeNode == null ? null : decimal(gradeNode, "medianPrice", "median", "price");
        BigDecimal smart = gradeNode == null ? null : decimal(gradeNode, "smartMarketPrice", "marketPrice");
        Integer sampleSize = gradeNode == null
                ? integer(card, "psa" + grade + "SampleSize", "psa" + grade + "SalesCount")
                : integer(gradeNode, "sampleSize", "salesCount", "sales", "count");
        String currency = gradeNode == null
                ? text(card, "currency", "sourceCurrency")
                : text(gradeNode, "currency", "sourceCurrency");
        return new ExternalPricingGradedPriceView("PSA " + grade, average, median, smart, sampleSize, currency);
    }

    private JsonNode gradeNode(JsonNode card, String grade) {
        String[] gradeKeys = {"PSA_" + grade, "psa_" + grade, grade, "psa" + grade};
        String[] roots = {"ebay", "psa", "graded", "prices.ebay", "pricing.ebay"};
        for (String root : roots) {
            JsonNode node = node(card, root);
            if (node == null || node.isMissingNode()) {
                continue;
            }
            for (String gradeKey : gradeKeys) {
                JsonNode gradeValue = node.path(gradeKey);
                if (!gradeValue.isMissingNode() && !gradeValue.isNull()) {
                    return gradeValue;
                }
            }
            JsonNode psaNode = node.path("psa");
            for (String gradeKey : gradeKeys) {
                JsonNode gradeValue = psaNode.path(gradeKey);
                if (!gradeValue.isMissingNode() && !gradeValue.isNull()) {
                    return gradeValue;
                }
            }
        }
        return null;
    }

    private boolean hasPsaPrice(JsonNode card) {
        return gradeNode(card, "8") != null
                || gradeNode(card, "9") != null
                || gradeNode(card, "10") != null
                || decimal(card, "psa8Price", "psa_8", "PSA_8") != null
                || decimal(card, "psa9Price", "psa_9", "PSA_9") != null
                || decimal(card, "psa10Price", "psa_10", "PSA_10") != null;
    }

    private String gradedStatus(
            ExternalPricingGradedPriceView psa8,
            ExternalPricingGradedPriceView psa9,
            ExternalPricingGradedPriceView psa10,
            JsonNode card) {
        if (psa8 != null || psa9 != null || psa10 != null) {
            return "Graded data returned successfully.";
        }
        String limitation = planOrQuotaLimitation(card);
        if (limitation != null) {
            return limitation;
        }
        return "Graded data absent for this card or not included in the current plan.";
    }

    private String planOrQuotaLimitation(JsonNode node) {
        String message = text(node, "message", "error", "planLimitation", "quotaMessage");
        if (message == null) {
            return null;
        }
        String normalized = message.toLowerCase();
        if (normalized.contains("plan") || normalized.contains("subscription") || normalized.contains("premium")) {
            return "Subscription plan blocked graded or premium data: " + message;
        }
        if (normalized.contains("quota") || normalized.contains("credit") || normalized.contains("limit")) {
            return "Quota or daily credit limit message: " + message;
        }
        return message;
    }

    private ConfidenceRating confidence(BigDecimal displayRaw, Integer sampleSize) {
        if (displayRaw == null) {
            return ConfidenceRating.LOW;
        }
        if (sampleSize != null && sampleSize >= 10) {
            return ConfidenceRating.MEDIUM;
        }
        return ConfidenceRating.LOW;
    }

    private void requireEnabledAndConfigured() {
        if (!properties.isEnabled()) {
            throw new PokemonPriceTrackerProviderException("PokemonPriceTracker provider is disabled.");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new PokemonPriceTrackerProviderException("PokemonPriceTracker API key is not configured.");
        }
    }

    private String requireSearchOrId(String searchOrId) {
        if (searchOrId == null || searchOrId.isBlank()) {
            throw new IllegalArgumentException("Card id or search text is required.");
        }
        return searchOrId.trim();
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
        if (status == 401) {
            return "PokemonPriceTracker authentication failed.";
        }
        if (status == 403) {
            return "PokemonPriceTracker subscription plan blocked this request or data field.";
        }
        if (status == 404) {
            return "PokemonPriceTracker card was not found.";
        }
        if (status == 429) {
            return "PokemonPriceTracker daily credit limit or rate limit was exceeded.";
        }
        if (status >= 500) {
            return "PokemonPriceTracker upstream service failed.";
        }
        return body == null || body.isBlank()
                ? "PokemonPriceTracker request failed with status " + status + "."
                : "PokemonPriceTracker request failed with status " + status + ".";
    }

    private String text(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode value = node(node, path);
            if (value != null && !value.isMissingNode() && !value.isNull()) {
                if (value.isTextual() && !value.asText().isBlank()) {
                    return value.asText().trim();
                }
                if (value.isNumber() || value.isBoolean()) {
                    return value.asText();
                }
            }
        }
        return null;
    }

    private BigDecimal decimal(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode value = node(node, path);
            if (value != null && !value.isMissingNode() && !value.isNull()) {
                if (value.isNumber()) {
                    return value.decimalValue();
                }
                if (value.isTextual() && !value.asText().isBlank()) {
                    try {
                        return new BigDecimal(value.asText().trim());
                    } catch (NumberFormatException ignored) {
                        // Try the next configured field.
                    }
                }
            }
        }
        return null;
    }

    private Integer integer(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode value = node(node, path);
            if (value != null && !value.isMissingNode() && !value.isNull()) {
                if (value.isInt() || value.isLong()) {
                    return value.asInt();
                }
                if (value.isTextual() && !value.asText().isBlank()) {
                    try {
                        return Integer.valueOf(value.asText().trim());
                    } catch (NumberFormatException ignored) {
                        // Try the next configured field.
                    }
                }
            }
        }
        return null;
    }

    private JsonNode node(JsonNode node, String path) {
        JsonNode current = node;
        Iterator<String> parts = java.util.Arrays.stream(path.split("\\.")).iterator();
        while (parts.hasNext() && current != null) {
            current = current.path(parts.next());
        }
        return current;
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
}
