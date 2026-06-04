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
    void alertsTrackLifecycleStatus() {
        assertThat(AlertStatus.values()).containsExactly(
                AlertStatus.NEW,
                AlertStatus.ACTIVE,
                AlertStatus.ACKNOWLEDGED,
                AlertStatus.DISMISSED);
    }

    @Test
    void ownedItemsTrackDisposalLifecycleStatus() {
        assertThat(OwnedItemStatus.values()).containsExactly(
                OwnedItemStatus.ACTIVE,
                OwnedItemStatus.SOLD,
                OwnedItemStatus.TRADED,
                OwnedItemStatus.DELETED);
        assertThat(DisposalType.values()).containsExactly(
                DisposalType.SOLD,
                DisposalType.TRADED,
                DisposalType.DELETED);
    }

    @Test
    void moneyValuesRoundToTwoDecimals() {
        assertThat(MoneyCalculationSupport.money(new BigDecimal("10.555")))
                .isEqualByComparingTo("10.56");
    }
}
