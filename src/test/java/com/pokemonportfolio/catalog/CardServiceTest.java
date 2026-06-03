package com.pokemonportfolio.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CardServiceTest {

    @Autowired
    private CardService cardService;

    @Test
    void createsManualEnglishCard() {
        CardForm form = new CardForm();
        form.setName("Pikachu");
        form.setSetName("Scarlet & Violet Promos");
        form.setCardNumber("101");
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.PROMO);

        Card card = cardService.createManualCard(form);

        assertThat(card.getId()).isNotNull();
        assertThat(card.getName()).isEqualTo("Pikachu");
        assertThat(card.getPokemonSet().getName()).isEqualTo("Scarlet & Violet Promos");
        assertThat(card.getLanguageMarket()).isEqualTo(LanguageMarket.ENGLISH);
        assertThat(card.getVariant()).isEqualTo(CardVariant.PROMO);
        assertThat(card.getAvailableVariants()).containsExactly(CardVariant.PROMO);
        assertThat(card.getDefaultOwnedVariant()).isEqualTo(CardVariant.PROMO);
        assertThat(card.getCatalogSource()).isEqualTo(CatalogSource.MANUAL);
        assertThat(card.getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
    }
}
