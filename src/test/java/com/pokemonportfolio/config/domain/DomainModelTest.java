package com.pokemonportfolio.config.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DomainModelTest {

    @Test
    void verticalSliceUsesEnglishMarketOnly() {
        assertThat(LanguageMarket.values()).containsExactly(LanguageMarket.ENGLISH);
    }

    @Test
    void cardsTrackCatalogSourceAndVerificationStatus() {
        assertThat(CatalogSource.values()).containsExactly(CatalogSource.MANUAL, CatalogSource.POKEMON_TCG_API);
        assertThat(VerificationStatus.values()).containsExactly(VerificationStatus.UNVERIFIED, VerificationStatus.VERIFIED);
    }

    @Test
    void moneyValuesRoundToTwoDecimals() {
        assertThat(MoneyCalculationSupport.money(new BigDecimal("10.555")))
                .isEqualByComparingTo("10.56");
    }
}
