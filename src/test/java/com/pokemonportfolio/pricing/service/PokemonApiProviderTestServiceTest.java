package com.pokemonportfolio.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingCardView;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProperties;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProvider;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiPricingProviderException;
import com.pokemonportfolio.pricing.provider.pokemonapi.PokemonApiSealedProductPriceView;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PokemonApiProviderTestServiceTest {

    @Test
    void disabledProviderShowsFriendlyMessageWithoutCallingProvider() {
        PokemonApiPricingProperties properties = properties(false, "");
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        PokemonApiProviderTestService service = new PokemonApiProviderTestService(properties, provider);

        assertThatThrownBy(() -> service.test(cardForm("3852")))
                .isInstanceOf(PokemonApiProviderTestException.class)
                .hasMessage("Pokemon API provider is disabled. Set POKEMON_API_PRICING_ENABLED=true.");
        verifyNoInteractions(provider);
    }

    @Test
    void missingApiKeyShowsFriendlyMessageWithoutCallingProvider() {
        PokemonApiPricingProperties properties = properties(true, "");
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        PokemonApiProviderTestService service = new PokemonApiProviderTestService(properties, provider);

        assertThatThrownBy(() -> service.test(cardForm("3852")))
                .isInstanceOf(PokemonApiProviderTestException.class)
                .hasMessage("RapidAPI key is missing. Set POKEMON_API_RAPIDAPI_KEY.");
        verifyNoInteractions(provider);
    }

    @Test
    void successfulCardLookupReturnsDisplayOnlyResult() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.providerName()).thenReturn("PokemonApiPricingProvider");
        when(provider.fetchOneCard("3852")).thenReturn(cardView());
        PokemonApiProviderTestService service = new PokemonApiProviderTestService(
                properties(true, "test-key"),
                provider);

        PokemonApiProviderTestResultView result = service.test(cardForm("3852"));

        assertThat(result.lookupType()).isEqualTo(PokemonApiProviderTestLookupType.CARD);
        assertThat(result.sourceProvider()).isEqualTo("PokemonApiPricingProvider");
        assertThat(result.sourceUrl()).isEqualTo("https://pokemon-tcg-api.p.rapidapi.com/cards/3852");
        assertThat(result.card().cardName()).isEqualTo("Giratina VSTAR");
        assertThat(result.sealedProduct()).isNull();
    }

    @Test
    void successfulSealedProductLookupReturnsDisplayOnlyResult() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.providerName()).thenReturn("PokemonApiPricingProvider");
        when(provider.fetchProductById(20039L)).thenReturn(sealedProductView());
        PokemonApiProviderTestService service = new PokemonApiProviderTestService(
                properties(true, "test-key"),
                provider);

        PokemonApiProviderTestResultView result = service.test(sealedProductForm("20039"));

        assertThat(result.lookupType()).isEqualTo(PokemonApiProviderTestLookupType.SEALED_PRODUCT);
        assertThat(result.sourceUrl()).isEqualTo("https://pokemon-tcg-api.p.rapidapi.com/products/20039");
        assertThat(result.sealedProduct().productName()).isEqualTo("Crown Zenith Elite Trainer Box");
        assertThat(result.card()).isNull();
    }

    @Test
    void failedProviderCallShowsFriendlyError() {
        PokemonApiPricingProvider provider = mock(PokemonApiPricingProvider.class);
        when(provider.fetchOneCard("3852")).thenThrow(new PokemonApiPricingProviderException("Upstream unavailable."));
        PokemonApiProviderTestService service = new PokemonApiProviderTestService(
                properties(true, "test-key"),
                provider);

        assertThatThrownBy(() -> service.test(cardForm("3852")))
                .isInstanceOf(PokemonApiProviderTestException.class)
                .hasMessage("Pokemon API test request failed. Upstream unavailable.");
    }

    private PokemonApiProviderTestForm cardForm(String searchOrId) {
        PokemonApiProviderTestForm form = new PokemonApiProviderTestForm();
        form.setLookupType(PokemonApiProviderTestLookupType.CARD);
        form.setSearchOrId(searchOrId);
        return form;
    }

    private PokemonApiProviderTestForm sealedProductForm(String productId) {
        PokemonApiProviderTestForm form = new PokemonApiProviderTestForm();
        form.setLookupType(PokemonApiProviderTestLookupType.SEALED_PRODUCT);
        form.setSearchOrId(productId);
        return form;
    }

    private PokemonApiPricingProperties properties(boolean enabled, String apiKey) {
        PokemonApiPricingProperties properties = new PokemonApiPricingProperties();
        properties.setEnabled(enabled);
        properties.setBaseUrl("https://pokemon-tcg-api.p.rapidapi.com");
        properties.setRapidApiKey(apiKey);
        return properties;
    }

    private PokemonApiPricingCardView cardView() {
        return new PokemonApiPricingCardView(
                3852L,
                "Giratina VSTAR",
                "Crown Zenith",
                "GG69",
                "https://images.example/giratina.png",
                "USD",
                new BigDecimal("146.69"),
                new BigDecimal("163.71"),
                null,
                null,
                null);
    }

    private PokemonApiSealedProductPriceView sealedProductView() {
        return new PokemonApiSealedProductPriceView(
                20039L,
                "Crown Zenith Elite Trainer Box",
                "Crown Zenith",
                "https://images.example/etb.png",
                "EUR",
                new BigDecimal("89.99"),
                null,
                null,
                null,
                null,
                "USD",
                new BigDecimal("105.25"),
                new BigDecimal("112.00"));
    }
}
