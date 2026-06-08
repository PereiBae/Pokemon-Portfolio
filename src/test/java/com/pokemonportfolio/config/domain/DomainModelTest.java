package com.pokemonportfolio.config.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DomainModelTest {

    @Test
    void languageMarketsSupportSealedProductsWhileCardsRemainEnglishFirst() {
        assertThat(LanguageMarket.values()).containsExactly(
                LanguageMarket.ENGLISH,
                LanguageMarket.JAPANESE,
                LanguageMarket.CHINESE,
                LanguageMarket.OTHER);
    }

    @Test
    void sealedProductsTrackTypeAndCondition() {
        assertThat(SealedProductType.values()).containsExactly(
                SealedProductType.BOOSTER_BOX,
                SealedProductType.BOOSTER_PACK,
                SealedProductType.ETB,
                SealedProductType.COLLECTION_BOX,
                SealedProductType.PROMO_BOX,
                SealedProductType.OTHER);
        assertThat(SealedProductCondition.values()).containsExactly(
                SealedProductCondition.SEALED,
                SealedProductCondition.DAMAGED_SEALED,
                SealedProductCondition.OPENED,
                SealedProductCondition.OTHER);
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
    void pricingProviderResultsTrackRawAndPsaOutputs() {
        assertThat(PricingResultType.values()).containsExactly(
                PricingResultType.RAW_CARD,
                PricingResultType.PSA_8,
                PricingResultType.PSA_9,
                PricingResultType.PSA_10);
    }

    @Test
    void pricingResultsTrackVariantMatchClassification() {
        assertThat(PricingMatchClassification.values()).containsExactly(
                PricingMatchClassification.EXACT_VARIANT_MATCH,
                PricingMatchClassification.GENERIC_RAW_FALLBACK,
                PricingMatchClassification.UNSAFE_VARIANT_MISMATCH,
                PricingMatchClassification.NO_PRICE_AVAILABLE);
        assertThat(PricingMatchClassification.fromMetadata("source_field=x;match=GENERIC_RAW_FALLBACK"))
                .contains(PricingMatchClassification.GENERIC_RAW_FALLBACK);
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
