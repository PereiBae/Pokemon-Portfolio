package com.pokemonportfolio.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.SealedProductForm;
import com.pokemonportfolio.catalog.service.SealedProductService;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.SealedProductCondition;
import com.pokemonportfolio.config.domain.SealedProductType;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.entity.PortfolioValuationSnapshot;
import com.pokemonportfolio.portfolio.repository.PortfolioValuationSnapshotRepository;
import com.pokemonportfolio.portfolio.service.OwnedItemDisposalForm;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioDisposalService;
import com.pokemonportfolio.portfolio.service.PortfolioValuationService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PortfolioHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private SealedProductService sealedProductService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private PortfolioValuationService valuationService;

    @Autowired
    private PortfolioDisposalService disposalService;

    @Autowired
    private PortfolioValuationSnapshotRepository valuationSnapshotRepository;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Test
    @WithUserDetails("owner@example.com")
    void portfolioHistoryPageRendersAndFiltersRanges() throws Exception {
        AppUser owner = owner();
        valuationSnapshotRepository.save(portfolioSnapshot(owner, "111.00", OffsetDateTime.now().minusDays(45)));
        valuationSnapshotRepository.save(portfolioSnapshot(owner, "222.00", OffsetDateTime.now().minusDays(2)));

        mockMvc.perform(get("/portfolio/history").param("range", "7D"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Portfolio History")))
                .andExpect(content().string(containsString("7D")))
                .andExpect(content().string(containsString("1M")))
                .andExpect(content().string(containsString("3M")))
                .andExpect(content().string(containsString("1Y")))
                .andExpect(content().string(containsString("All")))
                .andExpect(content().string(containsString("SGD 222.00")))
                .andExpect(content().string(not(containsString("SGD 111.00"))));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void portfolioHistoryShowsEmptyStateWhenHistoryIsTooSparse() throws Exception {
        mockMvc.perform(get("/portfolio/history"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Not enough portfolio history yet.")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void dashboardChartUsesStoredValuationSnapshots() throws Exception {
        AppUser owner = owner();
        valuationSnapshotRepository.save(portfolioSnapshot(owner, "100.00", OffsetDateTime.now().minusDays(2)));
        valuationSnapshotRepository.save(portfolioSnapshot(owner, "125.00", OffsetDateTime.now().minusDays(1)));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Stored valuation snapshots only")))
                .andExpect(content().string(containsString("<polyline")))
                .andExpect(content().string(containsString("2 stored snapshots")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void priceHistoryPageRendersOwnedCardSnapshots() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedCard(owner, "History Card", "50.00");
        priceSnapshot(item, "100.00", OffsetDateTime.now().minusDays(2), null);
        priceSnapshot(item, "120.00", OffsetDateTime.now().minusDays(1),
                "source_field=tcg_player.market_price;match=GENERIC_RAW_FALLBACK;variant=STANDARD;single_provider=true");

        mockMvc.perform(get("/portfolio/items/{id}/history", item.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("History Card")))
                .andExpect(content().string(containsString("Price History Graph")))
                .andExpect(content().string(containsString("Price Snapshot Table")))
                .andExpect(content().string(containsString("POKEMON_API")))
                .andExpect(content().string(containsString("TCGPLAYER")))
                .andExpect(content().string(containsString("USD")))
                .andExpect(content().string(containsString("LOW")))
                .andExpect(content().string(containsString("Generic raw fallback")))
                .andExpect(content().string(containsString("SGD 162.00")))
                .andExpect(content().string(containsString("<polyline")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void priceHistoryPageRendersSealedProductSnapshots() throws Exception {
        AppUser owner = owner();
        SealedProduct product = sealedProduct("History Sealed Box");
        OwnedItem item = ownedSealedProduct(owner, product, "180.00");
        priceSnapshotRepository.save(new PriceSnapshot(
                product,
                "MANUAL",
                new BigDecimal("220.00"),
                "SGD",
                BigDecimal.ONE.setScale(8),
                new BigDecimal("220.00"),
                ConfidenceRating.MEDIUM,
                "Manual sealed product price history test snapshot.",
                OffsetDateTime.now()));

        mockMvc.perform(get("/portfolio/items/{id}/history", item.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("History Sealed Box")))
                .andExpect(content().string(containsString("Sealed Product")))
                .andExpect(content().string(containsString("SGD 220.00")));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void priceHistoryShowsNoPriceAvailableWithoutZeroGainLoss() throws Exception {
        OwnedItem item = ownedCard(owner(), "History Unpriced Card", "88.00");

        mockMvc.perform(get("/portfolio/items/{id}/history", item.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("History Unpriced Card")))
                .andExpect(content().string(containsString("No price available")))
                .andExpect(content().string(not(containsString("SGD -88.00"))));
    }

    @Test
    @WithUserDetails("owner@example.com")
    void valuationSnapshotActionCreatesAppendOnlySnapshots() throws Exception {
        AppUser owner = owner();
        long before = valuationSnapshotRepository.findByOwnerOrderByCalculatedAtAscIdAsc(owner).size();

        mockMvc.perform(post("/portfolio/history/snapshot").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/portfolio/history?snapshotCreated"));
        mockMvc.perform(post("/portfolio/history/snapshot").with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(valuationSnapshotRepository.findByOwnerOrderByCalculatedAtAscIdAsc(owner))
                .hasSize((int) before + 2);
    }

    @Test
    void disposedItemsAreExcludedFromNewActiveSnapshotsAndOldSnapshotsRemainUnchanged() {
        AppUser owner = owner();
        OwnedItem item = ownedCard(owner, "History Disposal Card", "50.00");
        priceSnapshot(item, "90.00", OffsetDateTime.now(), null);

        PortfolioValuationSnapshot before = valuationService.createSnapshot(owner);
        disposalService.sellItem(owner, item.getId(), saleForm("100.00"));
        PortfolioValuationSnapshot after = valuationService.createSnapshot(owner);
        PortfolioValuationSnapshot unchanged = valuationSnapshotRepository.findById(before.getId()).orElseThrow();

        assertThat(unchanged.getTotalValueSgd()).isEqualByComparingTo("121.50");
        assertThat(after.getTotalValueSgd()).isEqualByComparingTo("0.00");
        assertThat(after.getTotalCostBasisSgd()).isEqualByComparingTo("0.00");
        assertThat(after.getItemCount()).isZero();
    }

    @Test
    @WithUserDetails("owner@example.com")
    void priceHistoryIndexRendersHoldingsAndLinks() throws Exception {
        ownedCard(owner(), "History Index Card", "35.00");

        mockMvc.perform(get("/pricing/history"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Price History")))
                .andExpect(content().string(containsString("History Index Card")))
                .andExpect(content().string(containsString("View History")))
                .andExpect(content().string(containsString("No price available")));
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private PortfolioValuationSnapshot portfolioSnapshot(AppUser owner, String value, OffsetDateTime calculatedAt) {
        BigDecimal totalValue = new BigDecimal(value);
        BigDecimal totalCost = new BigDecimal("80.00");
        BigDecimal unrealized = totalValue.subtract(totalCost);
        BigDecimal realized = new BigDecimal("10.00");
        BigDecimal totalPerformance = unrealized.add(realized);
        return new PortfolioValuationSnapshot(
                owner,
                totalValue,
                totalCost,
                unrealized,
                new BigDecimal("12.5000"),
                realized,
                new BigDecimal("10.0000"),
                new BigDecimal("100.00"),
                totalPerformance,
                new BigDecimal("18.0000"),
                2,
                0,
                calculatedAt.toLocalDate(),
                calculatedAt,
                "History controller test snapshot.");
    }

    private OwnedItem ownedCard(AppUser owner, String name, String purchasePrice) {
        Card card = card(name);
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(card.getId());
        form.setVariant(CardVariant.STANDARD);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        form.setPurchaseDate(LocalDate.of(2026, 6, 8));
        form.setGradedStatus(GradedStatus.UNGRADED);
        return ownedItemService.addCardToPortfolio(owner, form);
    }

    private OwnedItem ownedSealedProduct(AppUser owner, SealedProduct product, String purchasePrice) {
        OwnedItemForm form = new OwnedItemForm();
        form.setSealedProductId(product.getId());
        form.setSealedCondition(SealedProductCondition.SEALED);
        form.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        form.setPurchaseDate(LocalDate.of(2026, 6, 8));
        return ownedItemService.addSealedProductToPortfolio(owner, form);
    }

    private Card card(String name) {
        CardForm form = new CardForm();
        form.setName(name);
        form.setSetName("History Set " + System.nanoTime());
        form.setCardNumber("H-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
    }

    private SealedProduct sealedProduct(String name) {
        SealedProductForm form = new SealedProductForm();
        form.setName(name);
        form.setProductType(SealedProductType.BOOSTER_BOX);
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setSetName("History Sealed Set");
        return sealedProductService.createManualSealedProduct(form);
    }

    private PriceSnapshot priceSnapshot(
            OwnedItem item,
            String sourcePrice,
            OffsetDateTime calculatedAt,
            String providerMetadata) {
        BigDecimal source = new BigDecimal(sourcePrice);
        BigDecimal market = source.multiply(new BigDecimal("1.35000000")).setScale(2);
        return priceSnapshotRepository.save(new PriceSnapshot(
                item.getCard(),
                item.getOwnedVariant(),
                "POKEMON_API",
                "TCGPLAYER",
                source,
                "USD",
                new BigDecimal("1.35000000"),
                market,
                providerMetadata == null ? ConfidenceRating.MEDIUM : ConfidenceRating.LOW,
                providerMetadata == null
                        ? "Exact variant price history test snapshot."
                        : "Generic raw price used; provider did not supply variant-specific pricing.",
                calculatedAt,
                "https://example.test/source",
                null,
                providerMetadata));
    }

    private OwnedItemDisposalForm saleForm(String salePrice) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setSalePriceSgd(new BigDecimal(salePrice));
        form.setFeesSgd(BigDecimal.ZERO);
        form.setDisposalDate(LocalDate.of(2026, 6, 8));
        return form;
    }
}
