package com.pokemonportfolio.catalog.provider;

import com.pokemonportfolio.catalog.service.OfficialCardSearchResult;
import com.pokemonportfolio.catalog.service.OfficialCardSearchPage;
import com.pokemonportfolio.catalog.service.OfficialCardSearchRequest;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PokemonTcgApiCardCatalogueProvider implements CardCatalogueProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final DateTimeFormatter RELEASE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String SELECT_FIELDS = "id,name,number,rarity,set,images,tcgplayer";
    private static final Map<String, CardVariant> TCGPLAYER_VARIANT_KEYS = tcgplayerVariantKeys();

    private final PokemonTcgApiProperties properties;
    private final WebClient webClient;

    public PokemonTcgApiCardCatalogueProvider(PokemonTcgApiProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public CatalogSource source() {
        return CatalogSource.POKEMON_TCG_API;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public OfficialCardSearchPage search(OfficialCardSearchRequest request) {
        if (!isEnabled() || request == null || !request.hasCriteria()) {
            return request == null
                    ? new OfficialCardSearchPage(List.of(), 1, OfficialCardSearchRequest.DEFAULT_PAGE_SIZE, null)
                    : OfficialCardSearchPage.empty(request);
        }
        try {
            PokemonTcgSearchResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cards")
                            .queryParam("q", buildSearchQuery(request))
                            .queryParam("page", request.getPage())
                            .queryParam("pageSize", request.getPageSize())
                            .queryParam("select", SELECT_FIELDS)
                            .build())
                    .headers(headers -> addApiKey(headers::set))
                    .retrieve()
                    .bodyToMono(PokemonTcgSearchResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (response == null || response.data() == null) {
                return OfficialCardSearchPage.empty(request);
            }
            int page = response.page() == null ? request.getPage() : response.page();
            int pageSize = response.pageSize() == null ? request.getPageSize() : response.pageSize();
            return new OfficialCardSearchPage(
                    response.data().stream().map(this::toResult).toList(),
                    page,
                    pageSize,
                    response.totalCount());
        } catch (RuntimeException ex) {
            throw new CardCatalogueProviderException("Pokemon TCG API is unavailable right now.", ex);
        }
    }

    @Override
    public OfficialCardSearchResult findByExternalId(String externalCardId) {
        if (!isEnabled()) {
            throw new CardCatalogueProviderException("Pokemon TCG API provider is disabled.");
        }
        try {
            PokemonTcgCardResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cards/{id}")
                            .queryParam("select", SELECT_FIELDS)
                            .build(externalCardId))
                    .headers(headers -> addApiKey(headers::set))
                    .retrieve()
                    .bodyToMono(PokemonTcgCardResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (response == null || response.data() == null) {
                throw new CardCatalogueProviderException("Official card could not be found.");
            }
            return toResult(response.data());
        } catch (CardCatalogueProviderException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new CardCatalogueProviderException("Pokemon TCG API is unavailable right now.", ex);
        }
    }

    private void addApiKey(HeaderSetter headerSetter) {
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headerSetter.set("X-Api-Key", properties.getApiKey().trim());
        }
    }

    private String buildSearchQuery(OfficialCardSearchRequest request) {
        List<String> clauses = new ArrayList<>();
        if (!isBlank(request.getKeyword())) {
            for (String token : request.getKeyword().split("\\s+")) {
                if (!token.isBlank()) {
                    clauses.add("("
                            + containsClause("name", token)
                            + " OR "
                            + containsClause("set.name", token)
                            + " OR "
                            + exactClause("number", token)
                            + ")");
                }
            }
        }
        addExactClause(clauses, "name", request.getCardName());
        addExactClause(clauses, "set.name", request.getSetName());
        addExactClause(clauses, "number", request.getCardNumber());
        addExactClause(clauses, "rarity", request.getRarity());
        return String.join(" ", clauses);
    }

    private void addExactClause(List<String> clauses, String field, String value) {
        if (!isBlank(value)) {
            clauses.add(exactClause(field, value));
        }
    }

    private String exactClause(String field, String value) {
        return field + ":\"" + escapeQueryValue(value) + "\"";
    }

    private String containsClause(String field, String value) {
        return field + ":*" + escapeWildcardValue(value) + "*";
    }

    private String escapeQueryValue(String value) {
        return value.trim()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String escapeWildcardValue(String value) {
        return escapeQueryValue(value)
                .replace(":", "\\:")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private OfficialCardSearchResult toResult(PokemonTcgCard card) {
        PokemonTcgSet set = card.set();
        PokemonTcgImages images = card.images();
        String setName = set == null || isBlank(set.name()) ? "Unknown Set" : set.name();
        String imageSmallUrl = images == null ? null : blankToNull(images.small());
        String imageLargeUrl = images == null ? null : blankToNull(images.large());
        String externalCardUrl = card.tcgplayer() == null ? null : card.tcgplayer().url();
        return new OfficialCardSearchResult(
                card.id(),
                card.name(),
                set == null ? null : blankToNull(set.id()),
                setName,
                set == null ? null : blankToNull(set.series()),
                set == null ? null : parseReleaseDate(set.releaseDate()),
                card.number(),
                card.rarity(),
                imageSmallUrl,
                imageLargeUrl,
                externalCardUrl,
                LanguageMarket.ENGLISH,
                CatalogSource.POKEMON_TCG_API,
                availableVariants(card));
    }

    private List<CardVariant> availableVariants(PokemonTcgCard card) {
        if (card.tcgplayer() == null || card.tcgplayer().prices() == null || card.tcgplayer().prices().isEmpty()) {
            return List.of();
        }
        List<CardVariant> variants = new ArrayList<>();
        for (Map.Entry<String, CardVariant> entry : TCGPLAYER_VARIANT_KEYS.entrySet()) {
            if (card.tcgplayer().prices().containsKey(entry.getKey())
                    && card.tcgplayer().prices().get(entry.getKey()) != null) {
                variants.add(entry.getValue());
            }
        }
        return List.copyOf(variants);
    }

    private LocalDate parseReleaseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value, RELEASE_DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Map<String, CardVariant> tcgplayerVariantKeys() {
        Map<String, CardVariant> variants = new LinkedHashMap<>();
        variants.put("normal", CardVariant.STANDARD);
        variants.put("holofoil", CardVariant.HOLO);
        variants.put("reverseHolofoil", CardVariant.REVERSE_HOLO);
        variants.put("1stEditionHolofoil", CardVariant.FIRST_EDITION_HOLO);
        variants.put("1stEditionNormal", CardVariant.FIRST_EDITION_NORMAL);
        variants.put("unlimitedHolofoil", CardVariant.UNLIMITED_HOLO);
        variants.put("unlimitedNormal", CardVariant.UNLIMITED_NORMAL);
        return Collections.unmodifiableMap(variants);
    }

    private interface HeaderSetter {
        void set(String headerName, String headerValue);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonTcgSearchResponse(
            List<PokemonTcgCard> data,
            Integer page,
            Integer pageSize,
            Integer count,
            Integer totalCount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonTcgCardResponse(PokemonTcgCard data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonTcgCard(
            String id,
            String name,
            String number,
            String rarity,
            PokemonTcgSet set,
            PokemonTcgImages images,
            PokemonTcgLink tcgplayer) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonTcgSet(String id, String name, String series, String releaseDate) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonTcgImages(String small, String large) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PokemonTcgLink(String url, Map<String, Object> prices) {}
}
