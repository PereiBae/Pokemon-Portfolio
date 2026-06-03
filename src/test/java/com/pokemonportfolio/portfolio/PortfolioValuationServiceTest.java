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
import com.pokemonportfolio.portfolio.repository.PortfolioValuationSnapshotRepository;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioValuationService;
import com.pokemonportfolio.pricing.service.MarketValuationService;
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
class PortfolioValuationServiceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private MarketValuationService marketValuationService;

    @Autowired
    private PortfolioValuationService portfolioValuationService;

    @Autowired
    private PortfolioValuationSnapshotRepository valuationSnapshotRepository;

    @Test
    void calculatesCurrentPortfolioValueAndStoresHistoricalSnapshot() {
        var owner = appUserRepository.findByUsername("owner@example.com").orElseThrow();
        Card card = createCard();
        ownedItemService.addCardToPortfolio(owner, ownedItemForm(card.getId()));
        var price = marketValuationService.refreshCardPrice(card);

        var view = portfolioValuationService.calculateCurrentValue(owner);
        var firstSnapshot = portfolioValuationService.createSnapshot(owner);
        var secondSnapshot = portfolioValuationService.createSnapshot(owner);

        assertThat(view.totalValueSgd()).isEqualByComparingTo(price.getMarketPriceSgd());
        assertThat(view.totalCostBasisSgd()).isEqualByComparingTo("50.00");
        assertThat(firstSnapshot.getId()).isNotEqualTo(secondSnapshot.getId());
        assertThat(valuationSnapshotRepository.findByOwnerOrderByCalculatedAtAscIdAsc(owner)).hasSize(2);
    }

    private Card createCard() {
        CardForm form = new CardForm();
        form.setName("Charizard");
        form.setSetName("VS1 Valuation Test");
        form.setCardNumber("199");
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.ALTERNATE_ART);
        return cardService.createManualCard(form);
    }

    private OwnedItemForm ownedItemForm(Long cardId) {
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(cardId);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(new BigDecimal("50.00"));
        form.setPurchaseDate(LocalDate.now());
        form.setGradedStatus(GradedStatus.UNGRADED);
        return form;
    }
}

