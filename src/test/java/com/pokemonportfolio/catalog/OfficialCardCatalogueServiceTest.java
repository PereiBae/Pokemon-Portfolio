package com.pokemonportfolio.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.repository.PokemonSetRepository;
import com.pokemonportfolio.catalog.provider.CardCatalogueProvider;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.OfficialCardCatalogueService;
import com.pokemonportfolio.catalog.service.OfficialCardSearchPage;
import com.pokemonportfolio.catalog.service.OfficialCardSearchRequest;
import com.pokemonportfolio.catalog.service.OfficialCardSearchResult;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.VerificationStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OfficialCardCatalogueServiceTest {

    @Autowired
    private OfficialCardCatalogueService officialCardCatalogueService;

    @Autowired
    private CardService cardService;

    @Autowired
    private PokemonSetRepository pokemonSetRepository;

    @Test
    void searchReturnsOfficialCardResultDtos() {
        List<OfficialCardSearchResult> results = officialCardCatalogueService.searchOfficialCards("Charizard");

        assertThat(results).hasSize(1);
        OfficialCardSearchResult result = results.getFirst();
        assertThat(result.getExternalCardId()).isEqualTo("sv3-223");
        assertThat(result.getName()).isEqualTo("Charizard ex");
        assertThat(result.getExternalSetId()).isEqualTo("sv3");
        assertThat(result.getSetName()).isEqualTo("Obsidian Flames");
        assertThat(result.getSetName()).isNotEqualTo("Unknown Set");
        assertThat(result.getSetSeries()).isEqualTo("Scarlet & Violet");
        assertThat(result.getSetReleaseDate()).isEqualTo(LocalDate.of(2023, 8, 11));
        assertThat(result.getAvailableVariants()).containsExactly(
                CardVariant.STANDARD,
                CardVariant.HOLO,
                CardVariant.REVERSE_HOLO);
        assertThat(result.getSource()).isEqualTo(CatalogSource.POKEMON_TCG_API);
        assertThat(result.getLanguageMarket()).isEqualTo(LanguageMarket.ENGLISH);
    }

    @Test
    void importingOfficialCardCreatesVerifiedLocalCard() {
        Card card = officialCardCatalogueService.importOfficialCard(CatalogSource.POKEMON_TCG_API, "sv3-223");

        assertThat(card.getId()).isNotNull();
        assertThat(card.getName()).isEqualTo("Charizard ex");
        assertThat(card.getPokemonSet().getName()).isEqualTo("Obsidian Flames");
        assertThat(card.getPokemonSet().getExternalSetId()).isEqualTo("sv3");
        assertThat(card.getPokemonSet().getSeries()).isEqualTo("Scarlet & Violet");
        assertThat(card.getPokemonSet().getReleaseDate()).isEqualTo(LocalDate.of(2023, 8, 11));
        assertThat(card.getCatalogSource()).isEqualTo(CatalogSource.POKEMON_TCG_API);
        assertThat(card.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(card.getExternalCardId()).isEqualTo("sv3-223");
        assertThat(card.getExternalImageSmallUrl()).isEqualTo("https://images.example/charizard-small.png");
        assertThat(card.getExternalImageLargeUrl()).isEqualTo("https://images.example/charizard-large.png");
        assertThat(card.getAvailableVariants()).containsExactly(
                CardVariant.STANDARD,
                CardVariant.HOLO,
                CardVariant.REVERSE_HOLO);
        assertThat(card.getRarity()).isEqualTo("Special Illustration Rare");
    }

    @Test
    void importingSameOfficialCardTwiceReusesExistingCard() {
        Card first = officialCardCatalogueService.importOfficialCard(CatalogSource.POKEMON_TCG_API, "sv3-223");
        Card second = officialCardCatalogueService.importOfficialCard(CatalogSource.POKEMON_TCG_API, "sv3-223");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(pokemonSetRepository.countByExternalSetIdAndLanguageMarket("sv3", LanguageMarket.ENGLISH))
                .isEqualTo(1);
    }

    @Test
    void manualCardCreationCreatesUnverifiedManualCard() {
        CardForm form = new CardForm();
        form.setName("Local Test Card");
        form.setSetName("Manual Set " + System.nanoTime());
        form.setCardNumber("M-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.PROMO);

        Card card = cardService.createManualCard(form);

        assertThat(card.getCatalogSource()).isEqualTo(CatalogSource.MANUAL);
        assertThat(card.getVerificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
        assertThat(card.getExternalCardId()).isNull();
    }

    @TestConfiguration
    static class FakeProviderConfig {

        @Bean
        CardCatalogueProvider fakeOfficialCardProvider() {
            return new CardCatalogueProvider() {
                @Override
                public CatalogSource source() {
                    return CatalogSource.POKEMON_TCG_API;
                }

                @Override
                public boolean isEnabled() {
                    return true;
                }

                @Override
                public OfficialCardSearchPage search(OfficialCardSearchRequest request) {
                    return new OfficialCardSearchPage(
                            List.of(charizard()),
                            request.getPage(),
                            request.getPageSize(),
                            1);
                }

                @Override
                public OfficialCardSearchResult findByExternalId(String externalCardId) {
                    return charizard();
                }

                private OfficialCardSearchResult charizard() {
                    return new OfficialCardSearchResult(
                            "sv3-223",
                            "Charizard ex",
                            "sv3",
                            "Obsidian Flames",
                            "Scarlet & Violet",
                            LocalDate.of(2023, 8, 11),
                            "223",
                            "Special Illustration Rare",
                            "https://images.example/charizard-small.png",
                            "https://images.example/charizard-large.png",
                            "https://prices.example/charizard",
                            LanguageMarket.ENGLISH,
                            CatalogSource.POKEMON_TCG_API,
                            List.of(CardVariant.STANDARD, CardVariant.HOLO, CardVariant.REVERSE_HOLO));
                }
            };
        }
    }
}
