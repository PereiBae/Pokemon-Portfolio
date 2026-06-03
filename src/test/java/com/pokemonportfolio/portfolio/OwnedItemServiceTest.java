package com.pokemonportfolio.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.portfolio.repository.OwnedItemRepository;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OwnedItemServiceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private OwnedItemRepository ownedItemRepository;

    @Test
    void addsEachOwnedCopyAsSeparatePortfolioRecord() {
        var owner = appUserRepository.findByUsername("owner@example.com").orElseThrow();
        Card card = createCard("Umbreon", "VS1 Portfolio Test", "161");

        ownedItemService.addCardToPortfolio(owner, form(card.getId(), "42.00"));
        ownedItemService.addCardToPortfolio(owner, form(card.getId(), "45.00"));

        assertThat(ownedItemRepository.countByCardId(card.getId())).isEqualTo(2);
        assertThat(ownedItemService.listActiveItems(owner))
                .extracting(item -> item.getPurchasePriceSgd().toPlainString())
                .contains("42.00", "45.00");
    }

    private Card createCard(String name, String setName, String number) {
        CardForm form = new CardForm();
        form.setName(name);
        form.setSetName(setName);
        form.setCardNumber(number);
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.ALTERNATE_ART);
        return cardService.createManualCard(form);
    }

    private OwnedItemForm form(Long cardId, String price) {
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(cardId);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(new BigDecimal(price));
        form.setPurchaseDate(LocalDate.now());
        form.setGradedStatus(GradedStatus.UNGRADED);
        return form;
    }
}

