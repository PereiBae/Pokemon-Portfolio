package com.pokemonportfolio.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ManualPriceEntryServiceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private ManualPriceEntryService manualPriceEntryService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Value("${app.owner.username}")
    private String ownerUsername;

    @Test
    void manualEntryCreatesAppendOnlySnapshotsWithCurrencyAuditFields() {
        AppUser owner = appUserRepository.findByUsername(ownerUsername).orElseThrow();
        Card card = createCard();

        var first = manualPriceEntryService.createManualSnapshot(owner, form(card.getId()));
        var second = manualPriceEntryService.createManualSnapshot(owner, form(card.getId()));

        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(priceSnapshotRepository.findByCardIdOrderByCalculatedAtDescIdDesc(card.getId()))
                .hasSize(2)
                .allSatisfy(snapshot -> {
                    assertThat(snapshot.getProviderName()).isEqualTo("MANUAL");
                    assertThat(snapshot.getSourceCurrency()).isEqualTo("USD");
                    assertThat(snapshot.getExchangeRateUsed()).isEqualByComparingTo("1.35000000");
                    assertThat(snapshot.getMarketPriceSgd()).isEqualByComparingTo("13.50");
                    assertThat(snapshot.getConfidenceRating()).isEqualTo(ConfidenceRating.LOW);
                });
    }

    private ManualPriceEntryForm form(Long cardId) {
        ManualPriceEntryForm form = new ManualPriceEntryForm();
        form.setCardId(cardId);
        form.setProviderName("manual");
        form.setSourcePrice(new BigDecimal("10.00"));
        form.setSourceCurrency("USD");
        form.setExchangeRateUsed(new BigDecimal("1.35000000"));
        form.setMarketPriceSgd(new BigDecimal("13.50"));
        form.setConfidenceRating(ConfidenceRating.LOW);
        form.setNotes("manual service test");
        return form;
    }

    private Card createCard() {
        CardForm form = new CardForm();
        form.setName("Mewtwo");
        form.setSetName("Manual Price Service Test");
        form.setCardNumber("MP-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.HOLO);
        return cardService.createManualCard(form);
    }
}
