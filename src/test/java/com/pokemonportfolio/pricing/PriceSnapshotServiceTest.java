package com.pokemonportfolio.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import com.pokemonportfolio.pricing.service.MarketValuationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PriceSnapshotServiceTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private MarketValuationService marketValuationService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Test
    void mockPriceSnapshotsAreAppendOnly() {
        Card card = createCard();

        var first = marketValuationService.refreshCardPrice(card);
        var second = marketValuationService.refreshCardPrice(card);

        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId()))
                .hasSize(2)
                .allSatisfy(snapshot -> {
                    assertThat(snapshot.getProviderName()).isEqualTo("MockPricingProvider");
                    assertThat(snapshot.getSourceCurrency()).isEqualTo("SGD");
                    assertThat(snapshot.getExchangeRateUsed()).isEqualByComparingTo("1.00000000");
                });
    }

    private Card createCard() {
        CardForm form = new CardForm();
        form.setName("Gengar");
        form.setSetName("VS1 Snapshot Test");
        form.setCardNumber("094");
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.HOLO);
        return cardService.createManualCard(form);
    }
}

