package com.pokemonportfolio.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.pricing.provider.MockPricingProviderAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MockPricingProviderAdapterTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private MockPricingProviderAdapter mockPricingProvider;

    @Test
    void returnsSgdOnlyMockPriceWithoutExternalApi() {
        Card card = createCard();

        var price = mockPricingProvider.fetchCardPrice(card);

        assertThat(price.providerName()).isEqualTo("MockPricingProvider");
        assertThat(price.sourceCurrency()).isEqualTo("SGD");
        assertThat(price.exchangeRateUsed()).isEqualByComparingTo("1.00000000");
        assertThat(price.marketPriceSgd()).isPositive();
    }

    private Card createCard() {
        CardForm form = new CardForm();
        form.setName("Rayquaza");
        form.setSetName("VS1 Mock Pricing Test");
        form.setCardNumber("177");
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.SECRET_RARE);
        return cardService.createManualCard(form);
    }
}

