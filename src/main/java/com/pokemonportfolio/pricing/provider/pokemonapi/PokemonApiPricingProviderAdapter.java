package com.pokemonportfolio.pricing.provider.pokemonapi;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.PricingMatchClassification;
import com.pokemonportfolio.config.domain.PricingResultType;
import com.pokemonportfolio.config.domain.VerificationStatus;
import com.pokemonportfolio.pricing.provider.PricingProviderAdapter;
import com.pokemonportfolio.pricing.provider.PricingProviderCardPrices;
import com.pokemonportfolio.pricing.provider.PricingProviderException;
import com.pokemonportfolio.pricing.provider.PricingProviderPrice;
import com.pokemonportfolio.pricing.provider.PricingProviderResultValue;
import com.pokemonportfolio.pricing.provider.PricingProviderSkipReason;
import com.pokemonportfolio.pricing.service.CurrencyConversionService;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class PokemonApiPricingProviderAdapter implements PricingProviderAdapter {

    public static final String PROVIDER_NAME = "POKEMON_API";
    private static final String SOURCE_MARKET_TCGPLAYER = "TCGPLAYER";
    private static final String SOURCE_MARKET_CARDMARKET = "CARDMARKET";
    private static final String SOURCE_MARKET_EBAY_SOLD_GRADED = "EBAY_SOLD_GRADED";
    private static final int PSA_MEDIUM_SAMPLE_SIZE = 5;
    private static final Set<CardVariant> GENERIC_RAW_COMPATIBLE_VARIANTS = EnumSet.of(
            CardVariant.STANDARD,
            CardVariant.HOLO,
            CardVariant.REVERSE_HOLO);

    private final PokemonApiPricingProvider provider;
    private final PokemonApiPricingProperties properties;
    private final CurrencyConversionService currencyConversionService;

    public PokemonApiPricingProviderAdapter(
            PokemonApiPricingProvider provider,
            PokemonApiPricingProperties properties,
            CurrencyConversionService currencyConversionService) {
        this.provider = provider;
        this.properties = properties;
        this.currencyConversionService = currencyConversionService;
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled()
                && properties.getRapidApiKey() != null
                && !properties.getRapidApiKey().isBlank();
    }

    @Override
    public String unavailableMessage() {
        if (!properties.isEnabled()) {
            return "Pokemon API provider is disabled. Set POKEMON_API_PRICING_ENABLED=true.";
        }
        if (properties.getRapidApiKey() == null || properties.getRapidApiKey().isBlank()) {
            return "RapidAPI key is missing. Set POKEMON_API_RAPIDAPI_KEY.";
        }
        return "Pokemon API provider is unavailable.";
    }

    @Override
    public PricingProviderPrice fetchCardPrice(Card card) {
        return fetchCardPrices(card, CardVariant.STANDARD).rawPrice();
    }

    @Override
    public PricingProviderPrice fetchCardPrice(Card card, CardVariant variant) {
        return fetchCardPrices(card, variant).rawPrice();
    }

    @Override
    public PricingProviderCardPrices fetchCardPrices(Card card, CardVariant variant) {
        validateCard(card);
        CardVariant selectedVariant = variant == null ? CardVariant.STANDARD : variant;

        PokemonApiPricingCardView providerCard = fetchMatchedCard(card);
        PricingProviderPrice rawPrice = rawPrice(providerCard, selectedVariant);
        List<PricingProviderResultValue> results = new ArrayList<>();
        results.add(rawResult(rawPrice));
        psaResult(providerCard.psa8Price(), PricingResultType.PSA_8, providerCard).ifPresent(results::add);
        psaResult(providerCard.psa9Price(), PricingResultType.PSA_9, providerCard).ifPresent(results::add);
        psaResult(providerCard.psa10Price(), PricingResultType.PSA_10, providerCard).ifPresent(results::add);
        return new PricingProviderCardPrices(rawPrice, results);
    }

    private void validateCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Card is required");
        }
        if (card.getVerificationStatus() != VerificationStatus.VERIFIED
                || card.getCatalogSource() != CatalogSource.POKEMON_TCG_API) {
            throw new PricingProviderException(
                    PricingProviderSkipReason.NO_PROVIDER_MATCH,
                    "Only verified official Pokemon TCG API cards can use Pokemon API pricing.");
        }
    }

    private PokemonApiPricingCardView fetchMatchedCard(Card card) {
        PokemonApiPricingCardView direct = fetchByCompatibleExternalId(card);
        if (direct != null && matches(card, direct)) {
            return direct;
        }
        String query = searchQuery(card);
        try {
            PokemonApiPricingCardView searched = provider.searchFirstCard(query)
                    .orElseThrow(() -> new PricingProviderException(
                            PricingProviderSkipReason.NO_PROVIDER_MATCH,
                            "Pokemon API returned no pricing match for " + query + "."));
            if (!matches(card, searched)) {
                throw new PricingProviderException(
                        PricingProviderSkipReason.NO_PROVIDER_MATCH,
                        "Pokemon API result did not confidently match card name, set, and number.");
            }
            return searched;
        } catch (PokemonApiPricingProviderException ex) {
            throw new PricingProviderException(
                    PricingProviderSkipReason.PROVIDER_ERROR,
                    "Pokemon API pricing lookup failed.",
                    ex);
        }
    }

    private PokemonApiPricingCardView fetchByCompatibleExternalId(Card card) {
        String externalCardId = card.getExternalCardId();
        if (externalCardId == null || !externalCardId.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return provider.fetchCardById(Long.valueOf(externalCardId));
        } catch (PokemonApiPricingProviderException ex) {
            return null;
        }
    }

    private PricingProviderPrice rawPrice(PokemonApiPricingCardView card, CardVariant selectedVariant) {
        return card.variantPrice(selectedVariant)
                .map(price -> exactVariantPrice(card, selectedVariant, price))
                .orElseGet(() -> genericRawPrice(card, selectedVariant));
    }

    private PricingProviderPrice exactVariantPrice(
            PokemonApiPricingCardView card,
            CardVariant selectedVariant,
            PokemonApiRawPriceView variantPrice) {
        boolean marketUsed = variantPrice.marketPrice() != null;
        BigDecimal sourcePrice = marketUsed ? variantPrice.marketPrice() : variantPrice.midPrice();
        if (sourcePrice == null) {
            throw new PricingProviderException(
                    PricingProviderSkipReason.NO_PRICE_AVAILABLE,
                    "Pokemon API returned an exact variant entry without a usable price.");
        }
        String sourceCurrency = currencyConversionService.normalizeCurrency(
                variantPrice.currency() == null ? card.tcgPlayerCurrency() : variantPrice.currency());
        BigDecimal exchangeRate = rateToSgd(sourceCurrency);
        BigDecimal priceSgd = currencyConversionService.convertToSgd(sourcePrice, sourceCurrency, exchangeRate);
        ConfidenceRating confidence = marketUsed ? ConfidenceRating.MEDIUM : ConfidenceRating.LOW;
        String sourceKey = variantPrice.sourceKey() == null ? selectedVariant.name() : variantPrice.sourceKey();
        String sourceField = "tcg_player." + sourceKey + (marketUsed ? ".market_price" : ".mid_price");
        return new PricingProviderPrice(
                PROVIDER_NAME,
                SOURCE_MARKET_TCGPLAYER,
                MoneyCalculationSupport.money(sourcePrice),
                sourceCurrency,
                exchangeRate,
                priceSgd,
                confidence,
                sourceUrl(card.cardId()),
                null,
                metadata(sourceField, PricingMatchClassification.EXACT_VARIANT_MATCH, selectedVariant),
                "Pokemon API returned a TCGPlayer price for the owned variant.");
    }

    private PricingProviderPrice genericRawPrice(PokemonApiPricingCardView card, CardVariant selectedVariant) {
        RawPriceCandidate candidate = rawPriceCandidate(card);
        if (candidate == null) {
            throw new PricingProviderException(
                    PricingProviderSkipReason.NO_PRICE_AVAILABLE,
                    "No supported raw price available.");
        }
        if (!GENERIC_RAW_COMPATIBLE_VARIANTS.contains(selectedVariant)) {
            throw new PricingProviderException(
                    PricingProviderSkipReason.UNSAFE_VARIANT_MISMATCH,
                    "Generic raw price exists, but variant requires exact pricing.");
        }
        String sourceCurrency = currencyConversionService.normalizeCurrency(candidate.sourceCurrency());
        BigDecimal exchangeRate = rateToSgd(sourceCurrency);
        BigDecimal priceSgd = currencyConversionService.convertToSgd(candidate.sourcePrice(), sourceCurrency, exchangeRate);
        return new PricingProviderPrice(
                PROVIDER_NAME,
                candidate.sourceMarket(),
                MoneyCalculationSupport.money(candidate.sourcePrice()),
                sourceCurrency,
                exchangeRate,
                priceSgd,
                ConfidenceRating.LOW,
                sourceUrl(card.cardId()),
                null,
                metadata(candidate.sourceField(), PricingMatchClassification.GENERIC_RAW_FALLBACK, selectedVariant),
                "Generic raw price used; provider did not supply variant-specific pricing.");
    }

    private RawPriceCandidate rawPriceCandidate(PokemonApiPricingCardView card) {
        String tcgPlayerCurrency = blankToNull(card.tcgPlayerCurrency());
        String cardmarketCurrency = blankToNull(card.cardmarketCurrency());
        if (card.tcgPlayerMarketPrice() != null && tcgPlayerCurrency != null) {
            return new RawPriceCandidate(
                    SOURCE_MARKET_TCGPLAYER,
                    "tcg_player.market_price",
                    card.tcgPlayerMarketPrice(),
                    tcgPlayerCurrency);
        }
        if (card.tcgPlayerMidPrice() != null && tcgPlayerCurrency != null) {
            return new RawPriceCandidate(
                    SOURCE_MARKET_TCGPLAYER,
                    "tcg_player.mid_price",
                    card.tcgPlayerMidPrice(),
                    tcgPlayerCurrency);
        }
        if (card.cardmarketAveragePrice() != null && cardmarketCurrency != null) {
            return new RawPriceCandidate(
                    SOURCE_MARKET_CARDMARKET,
                    "cardmarket.average_price",
                    card.cardmarketAveragePrice(),
                    cardmarketCurrency);
        }
        if (card.cardmarketTrendPrice() != null && cardmarketCurrency != null) {
            return new RawPriceCandidate(
                    SOURCE_MARKET_CARDMARKET,
                    "cardmarket.trend_price",
                    card.cardmarketTrendPrice(),
                    cardmarketCurrency);
        }
        if (card.cardmarketLowPrice() != null && cardmarketCurrency != null) {
            return new RawPriceCandidate(
                    SOURCE_MARKET_CARDMARKET,
                    "cardmarket.low_price",
                    card.cardmarketLowPrice(),
                    cardmarketCurrency);
        }
        if (card.cardmarketAverageSellPrice() != null && cardmarketCurrency != null) {
            return new RawPriceCandidate(
                    SOURCE_MARKET_CARDMARKET,
                    "cardmarket.average_sell_price",
                    card.cardmarketAverageSellPrice(),
                    cardmarketCurrency);
        }
        return null;
    }

    private String metadata(
            String sourceField,
            PricingMatchClassification matchClassification,
            CardVariant selectedVariant) {
        return "source_field=" + sourceField
                + ";match=" + matchClassification.name()
                + ";variant=" + selectedVariant.name()
                + ";single_provider=true";
    }

    private PricingProviderResultValue rawResult(PricingProviderPrice rawPrice) {
        return new PricingProviderResultValue(
                rawPrice.providerName(),
                PricingResultType.RAW_CARD,
                rawPrice.sourceMarket(),
                rawPrice.sourcePrice(),
                rawPrice.sourceCurrency(),
                rawPrice.exchangeRateUsed(),
                rawPrice.marketPriceSgd(),
                rawPrice.confidenceRating(),
                null,
                rawPrice.sourceUrl(),
                rawPrice.sourceUpdatedAt(),
                rawPrice.providerMetadata());
    }

    private java.util.Optional<PricingProviderResultValue> psaResult(
            PokemonApiPsaPriceView psaPrice,
            PricingResultType resultType,
            PokemonApiPricingCardView card) {
        if (psaPrice == null || psaPrice.medianPrice() == null || psaPrice.currency() == null) {
            return java.util.Optional.empty();
        }
        String sourceCurrency = currencyConversionService.normalizeCurrency(psaPrice.currency());
        BigDecimal exchangeRate;
        try {
            exchangeRate = rateToSgd(sourceCurrency);
        } catch (PricingProviderException ex) {
            if (ex.getSkipReason() == PricingProviderSkipReason.MISSING_EXCHANGE_RATE) {
                return java.util.Optional.empty();
            }
            throw ex;
        }
        BigDecimal priceSgd = currencyConversionService.convertToSgd(
                psaPrice.medianPrice(),
                sourceCurrency,
                exchangeRate);
        ConfidenceRating confidence = psaPrice.sampleSize() != null && psaPrice.sampleSize() >= PSA_MEDIUM_SAMPLE_SIZE
                ? ConfidenceRating.MEDIUM
                : ConfidenceRating.LOW;
        return java.util.Optional.of(new PricingProviderResultValue(
                PROVIDER_NAME,
                resultType,
                SOURCE_MARKET_EBAY_SOLD_GRADED,
                MoneyCalculationSupport.money(psaPrice.medianPrice()),
                sourceCurrency,
                exchangeRate,
                priceSgd,
                confidence,
                psaPrice.sampleSize(),
                sourceUrl(card.cardId()),
                null,
                "graded_company=PSA;grade=" + psaPrice.grade() + ";single_provider=true"));
    }

    private BigDecimal rateToSgd(String sourceCurrency) {
        return currencyConversionService.latestRateToSgd(sourceCurrency)
                .orElseThrow(() -> new PricingProviderException(
                        PricingProviderSkipReason.MISSING_EXCHANGE_RATE,
                        "Missing " + sourceCurrency + " to SGD exchange rate.",
                        sourceCurrency));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean matches(Card card, PokemonApiPricingCardView providerCard) {
        return same(card.getName(), providerCard.cardName())
                && same(card.getPokemonSet().getName(), providerCard.expansionName())
                && sameNumber(card.getCardNumber(), providerCard.cardNumber());
    }

    private boolean same(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

    private boolean sameNumber(String expected, String actual) {
        return normalizeNumber(expected).equals(normalizeNumber(actual));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String normalizeNumber(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replace("#", "").toLowerCase(Locale.ROOT);
        int slashIndex = normalized.indexOf('/');
        if (slashIndex > 0) {
            normalized = normalized.substring(0, slashIndex);
        }
        return normalized;
    }

    private String searchQuery(Card card) {
        return card.getName() + " " + card.getPokemonSet().getName() + " " + card.getCardNumber();
    }

    private String sourceUrl(Long cardId) {
        String baseUrl = properties.getBaseUrl();
        String path = "/cards/" + cardId;
        if (baseUrl == null || baseUrl.isBlank()) {
            return path;
        }
        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) + path
                : baseUrl + path;
    }

    private record RawPriceCandidate(
            String sourceMarket,
            String sourceField,
            BigDecimal sourcePrice,
            String sourceCurrency) {
    }
}
