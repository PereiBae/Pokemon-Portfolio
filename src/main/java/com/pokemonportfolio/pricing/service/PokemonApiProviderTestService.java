package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingCardView;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProperties;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProvider;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProviderException;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiSealedProductPriceView;
import org.springframework.stereotype.Service;

@Service
public class PokemonApiProviderTestService {

    private static final String DISABLED_MESSAGE =
            "Pokemon API provider is disabled. Set POKEMON_API_PRICING_ENABLED=true.";
    private static final String MISSING_KEY_MESSAGE =
            "RapidAPI key is missing. Set POKEMON_API_RAPIDAPI_KEY.";

    private final PokemonApiPricingProperties properties;
    private final PokemonApiPricingProvider provider;

    public PokemonApiProviderTestService(
            PokemonApiPricingProperties properties,
            PokemonApiPricingProvider provider) {
        this.properties = properties;
        this.provider = provider;
    }

    public PokemonApiProviderStatusView status() {
        return new PokemonApiProviderStatusView(
                provider.providerName(),
                properties.isEnabled(),
                apiKeyConfigured(),
                properties.getBaseUrl());
    }

    public PokemonApiProviderTestResultView test(PokemonApiProviderTestForm form) {
        ensureUsable();
        PokemonApiProviderTestLookupType lookupType = form.getLookupType() == null
                ? PokemonApiProviderTestLookupType.CARD
                : form.getLookupType();
        String searchOrId = requiredSearchOrId(form.getSearchOrId(), lookupType);
        try {
            return switch (lookupType) {
                case CARD -> cardResult(searchOrId);
                case SEALED_PRODUCT -> sealedProductResult(searchOrId);
            };
        } catch (PokemonApiProviderTestException ex) {
            throw ex;
        } catch (PokemonApiPricingProviderException ex) {
            throw new PokemonApiProviderTestException("Pokemon API test request failed. " + ex.getMessage());
        } catch (RuntimeException ex) {
            throw new PokemonApiProviderTestException("Pokemon API test request failed.");
        }
    }

    private PokemonApiProviderTestResultView cardResult(String searchOrId) {
        PokemonApiPricingCardView card = provider.fetchOneCard(searchOrId);
        return new PokemonApiProviderTestResultView(
                PokemonApiProviderTestLookupType.CARD,
                provider.providerName(),
                sourceUrl("/cards/" + card.cardId()),
                card,
                null);
    }

    private PokemonApiProviderTestResultView sealedProductResult(String productIdText) {
        Long productId = parseProductId(productIdText);
        PokemonApiSealedProductPriceView sealedProduct = provider.fetchProductById(productId);
        return new PokemonApiProviderTestResultView(
                PokemonApiProviderTestLookupType.SEALED_PRODUCT,
                provider.providerName(),
                sourceUrl("/products/" + sealedProduct.productId()),
                null,
                sealedProduct);
    }

    private void ensureUsable() {
        if (!properties.isEnabled()) {
            throw new PokemonApiProviderTestException(DISABLED_MESSAGE);
        }
        if (!apiKeyConfigured()) {
            throw new PokemonApiProviderTestException(MISSING_KEY_MESSAGE);
        }
    }

    private boolean apiKeyConfigured() {
        return properties.getRapidApiKey() != null && !properties.getRapidApiKey().isBlank();
    }

    private String requiredSearchOrId(String searchOrId, PokemonApiProviderTestLookupType lookupType) {
        if (searchOrId == null || searchOrId.isBlank()) {
            return lookupType == PokemonApiProviderTestLookupType.CARD
                    ? "3852"
                    : throwBlankProductId();
        }
        return searchOrId.trim();
    }

    private String throwBlankProductId() {
        throw new PokemonApiProviderTestException("Sealed product id is required.");
    }

    private Long parseProductId(String productIdText) {
        try {
            return Long.valueOf(productIdText);
        } catch (NumberFormatException ex) {
            throw new PokemonApiProviderTestException("Sealed product lookup requires a numeric product id.");
        }
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
}
