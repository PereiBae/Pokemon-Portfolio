package com.pokemonportfolio.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemonportfolio.alerts.service.PriceAlertService;
import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.SealedProductForm;
import com.pokemonportfolio.catalog.service.SealedProductService;
import com.pokemonportfolio.config.domain.AssetType;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
import com.pokemonportfolio.config.domain.SealedProductCondition;
import com.pokemonportfolio.config.domain.SealedProductType;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.repository.OwnedItemRepository;
import com.pokemonportfolio.portfolio.service.OwnedItemDisposalForm;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioDisposalService;
import com.pokemonportfolio.portfolio.service.PortfolioValuationService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.repository.PriceSnapshotRepository;
import com.pokemonportfolio.pricing.service.ManualPriceEntryForm;
import com.pokemonportfolio.pricing.service.ManualPriceEntryService;
import com.pokemonportfolio.trade.service.TradeAnalyzerService;
import com.pokemonportfolio.trade.service.TradeCreateForm;
import com.pokemonportfolio.trade.service.TradeExecutionService;
import com.pokemonportfolio.trade.service.TradeIncomingItemForm;
import com.pokemonportfolio.trade.service.TradeOutgoingItemForm;
import com.pokemonportfolio.trade.service.TradeTransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SealedProductSupportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private SealedProductService sealedProductService;

    @Autowired
    private OwnedItemService ownedItemService;

    @Autowired
    private OwnedItemRepository ownedItemRepository;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private ManualPriceEntryService manualPriceEntryService;

    @Autowired
    private PortfolioValuationService valuationService;

    @Autowired
    private PortfolioDisposalService disposalService;

    @Autowired
    private PriceAlertService priceAlertService;

    @Autowired
    private CardService cardService;

    @Autowired
    private TradeTransactionService tradeTransactionService;

    @Autowired
    private TradeAnalyzerService tradeAnalyzerService;

    @Autowired
    private TradeExecutionService tradeExecutionService;

    @Test
    @WithUserDetails("owner@example.com")
    void sealedProductCreateAndAddPortfolioPagesWork() throws Exception {
        mockMvc.perform(get("/sealed-products/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create Sealed Product")));

        String redirect = mockMvc.perform(post("/sealed-products")
                        .with(csrf())
                        .param("name", "Prismatic Evolutions Booster Box")
                        .param("productType", "BOOSTER_BOX")
                        .param("languageMarket", "ENGLISH")
                        .param("setName", "Prismatic Evolutions")
                        .param("notes", "Local sealed test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/portfolio/add-sealed?sealedProductId=*"))
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        Long sealedProductId = Long.valueOf(redirect.substring(redirect.indexOf("sealedProductId=")
                + "sealedProductId=".length()));

        mockMvc.perform(get("/sealed-products"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Prismatic Evolutions Booster Box")))
                .andExpect(content().string(containsString("Image not available")));

        mockMvc.perform(post("/portfolio/sealed-items")
                        .with(csrf())
                        .param("sealedProductId", sealedProductId.toString())
                        .param("sealedCondition", "SEALED")
                        .param("purchasePriceSgd", "180.00")
                        .param("purchaseDate", "2026-06-04"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        assertThat(ownedItemRepository.countBySealedProductId(sealedProductId)).isEqualTo(1);
    }

    @Test
    void ownedSealedProductCopiesAreStoredAsSeparateRecords() {
        AppUser owner = owner();
        SealedProduct product = sealedProduct("Separate Copy Booster Box", null);

        OwnedItem first = ownedSealedProduct(owner, product, "100.00");
        OwnedItem second = ownedSealedProduct(owner, product, "125.00");

        assertThat(first.getAssetType()).isEqualTo(AssetType.SEALED_PRODUCT);
        assertThat(second.getAssetType()).isEqualTo(AssetType.SEALED_PRODUCT);
        assertThat(ownedItemRepository.countBySealedProductId(product.getId())).isEqualTo(2);
        assertThat(ownedItemService.listActiveItems(owner))
                .extracting(OwnedItem::getPurchasePriceSgd)
                .contains(new BigDecimal("100.00"), new BigDecimal("125.00"));
    }

    @Test
    void manualPriceEntryForSealedProductCreatesAppendOnlySnapshots() {
        AppUser owner = owner();
        SealedProduct product = sealedProduct("Manual Price Sealed Box", null);

        var first = manualPriceEntryService.createManualSnapshot(owner, manualPriceForm(product.getId()));
        var second = manualPriceEntryService.createManualSnapshot(owner, manualPriceForm(product.getId()));

        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(priceSnapshotRepository.findBySealedProductIdOrderByCalculatedAtDescIdDesc(product.getId()))
                .hasSize(2)
                .allSatisfy(snapshot -> {
                    assertThat(snapshot.getAssetType()).isEqualTo(AssetType.SEALED_PRODUCT);
                    assertThat(snapshot.getSourceCurrency()).isEqualTo("USD");
                    assertThat(snapshot.getExchangeRateUsed()).isEqualByComparingTo("1.35000000");
                    assertThat(snapshot.getMarketPriceSgd()).isEqualByComparingTo("135.00");
                });
    }

    @Test
    void dashboardIncludesActiveSealedProductsAndExcludesDisposedSealedProducts() {
        AppUser owner = owner();
        OwnedItem active = ownedSealedProduct(owner, sealedProduct("Active Sealed Box", null), "100.00");
        sealedSnapshot(active.getSealedProduct(), "150.00");
        OwnedItem sold = ownedSealedProduct(owner, sealedProduct("Sold Sealed Box", null), "100.00");
        OwnedItem traded = ownedSealedProduct(owner, sealedProduct("Traded Sealed Box", null), "80.00");
        OwnedItem deleted = ownedSealedProduct(owner, sealedProduct("Deleted Sealed Box", null), "90.00");

        disposalService.sellItem(owner, sold.getId(), saleForm("130.00", "0.00"));
        disposalService.tradeAwayItem(owner, traded.getId(), tradeForm("95.00"));
        disposalService.deleteMistake(owner, deleted.getId(), deleteForm("Wrong sealed product"));

        var view = valuationService.calculateCurrentValue(owner);

        assertThat(view.itemCount()).isEqualTo(1);
        assertThat(view.totalValueSgd()).isEqualByComparingTo("150.00");
        assertThat(view.totalCostBasisSgd()).isEqualByComparingTo("100.00");
        assertThat(view.unrealizedGainLossSgd()).isEqualByComparingTo("50.00");
        assertThat(view.realizedGainLossSgd()).isEqualByComparingTo("45.00");
        assertThat(active.getStatus()).isEqualTo(OwnedItemStatus.ACTIVE);
        assertThat(sold.getStatus()).isEqualTo(OwnedItemStatus.SOLD);
        assertThat(traded.getStatus()).isEqualTo(OwnedItemStatus.TRADED);
        assertThat(deleted.getStatus()).isEqualTo(OwnedItemStatus.DELETED);
    }

    @Test
    @WithUserDetails("owner@example.com")
    void alertsTriggerForSealedProductsAndRenderMissingImagePlaceholder() throws Exception {
        AppUser owner = owner();
        OwnedItem item = ownedSealedProduct(owner, sealedProduct("Alert Sealed Product", null), "80.00");
        sealedSnapshot(item.getSealedProduct(), "91.00");

        var alerts = priceAlertService.checkAlerts(owner);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().getGainAmountSgd()).isEqualByComparingTo("11.00");

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alert Sealed Product")))
                .andExpect(content().string(containsString("Image not available")));
    }

    @Test
    void tradeAnalyzerCanIncludeOutgoingSealedProduct() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedSealedProduct(owner, sealedProduct("Outgoing Sealed Trade Box", null), "100.00");
        sealedSnapshot(outgoing.getSealedProduct(), "180.00");
        Card incomingCard = card("Incoming Trade Card");
        var trade = tradeTransactionService.createDraft(owner, tradeForm());

        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), null));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingForm(incomingCard.getId(), "180.00"));
        var analysed = tradeAnalyzerService.analyze(owner, trade.getId());
        var detail = tradeTransactionService.detailView(owner, trade.getId());

        assertThat(analysed.getTotalOutgoingMarketValueSgd()).isEqualByComparingTo("180.00");
        assertThat(detail.outgoingItems().getFirst().itemDisplayName()).contains("Outgoing Sealed Trade Box");
        assertThat(detail.outgoingItems().getFirst().variantLabel()).isEqualTo("Booster Box");
    }

    @Test
    void tradeExecutionCanReceiveIncomingSealedProductWithAllocatedCostBasis() {
        AppUser owner = owner();
        OwnedItem outgoing = ownedSealedProduct(owner, sealedProduct("Outgoing For Incoming Sealed", null), "200.00");
        SealedProduct incoming = sealedProduct("Incoming Sealed Trade Box", null);
        var trade = tradeTransactionService.createDraft(owner, tradeForm());
        tradeTransactionService.addOutgoingItem(owner, trade.getId(), outgoingForm(outgoing.getId(), "500.00"));
        tradeTransactionService.addIncomingItem(owner, trade.getId(), incomingSealedForm(incoming.getId(), "500.00"));

        tradeExecutionService.execute(owner, trade.getId());
        var detail = tradeTransactionService.detailView(owner, trade.getId());
        Long incomingOwnedItemId = detail.incomingItems().getFirst().incomingOwnedItemId();
        OwnedItem incomingOwnedItem = ownedItemService.requireItemForOwner(owner, incomingOwnedItemId);

        assertThat(outgoing.getStatus()).isEqualTo(OwnedItemStatus.TRADED);
        assertThat(detail.incomingItems().getFirst().itemDisplayName()).contains("Incoming Sealed Trade Box");
        assertThat(detail.incomingItems().getFirst().allocatedCostBasisSgd()).isEqualByComparingTo("500.00");
        assertThat(incomingOwnedItem.getAssetType()).isEqualTo(AssetType.SEALED_PRODUCT);
        assertThat(incomingOwnedItem.getSealedProduct().getId()).isEqualTo(incoming.getId());
        assertThat(incomingOwnedItem.getPurchasePriceSgd()).isEqualByComparingTo("500.00");
        assertThat(incomingOwnedItem.getSealedCondition()).isEqualTo(SealedProductCondition.SEALED);
    }

    private AppUser owner() {
        return appUserRepository.findByUsername("owner@example.com").orElseThrow();
    }

    private SealedProduct sealedProduct(String name, String imageUrl) {
        SealedProductForm form = new SealedProductForm();
        form.setName(name + " " + System.nanoTime());
        form.setProductType(SealedProductType.BOOSTER_BOX);
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setSetName("Sealed Test Set");
        form.setImageUrl(imageUrl);
        return sealedProductService.createManualSealedProduct(form);
    }

    private OwnedItem ownedSealedProduct(AppUser owner, SealedProduct product, String purchasePrice) {
        OwnedItemForm form = new OwnedItemForm();
        form.setSealedProductId(product.getId());
        form.setSealedCondition(SealedProductCondition.SEALED);
        form.setPurchasePriceSgd(new BigDecimal(purchasePrice));
        form.setPurchaseDate(LocalDate.of(2026, 6, 4));
        return ownedItemService.addSealedProductToPortfolio(owner, form);
    }

    private ManualPriceEntryForm manualPriceForm(Long sealedProductId) {
        ManualPriceEntryForm form = new ManualPriceEntryForm();
        form.setSealedProductId(sealedProductId);
        form.setProviderName("manual");
        form.setSourcePrice(new BigDecimal("100.00"));
        form.setSourceCurrency("USD");
        form.setExchangeRateUsed(new BigDecimal("1.35000000"));
        form.setMarketPriceSgd(new BigDecimal("135.00"));
        form.setConfidenceRating(ConfidenceRating.LOW);
        return form;
    }

    private PriceSnapshot sealedSnapshot(SealedProduct product, String marketPrice) {
        return priceSnapshotRepository.save(new PriceSnapshot(
                product,
                "MANUAL",
                new BigDecimal(marketPrice),
                "SGD",
                BigDecimal.ONE,
                new BigDecimal(marketPrice),
                ConfidenceRating.MEDIUM,
                "Sealed product test snapshot.",
                OffsetDateTime.now()));
    }

    private Card card(String name) {
        CardForm form = new CardForm();
        form.setName(name + " " + System.nanoTime());
        form.setSetName("Sealed Trade Incoming Set " + System.nanoTime());
        form.setCardNumber("SP-" + System.nanoTime());
        form.setLanguageMarket(LanguageMarket.ENGLISH);
        form.setVariant(CardVariant.STANDARD);
        return cardService.createManualCard(form);
    }

    private TradeCreateForm tradeForm() {
        TradeCreateForm form = new TradeCreateForm();
        form.setName("Sealed Trade " + System.nanoTime());
        form.setOutgoingTradePercentage(new BigDecimal("100.00"));
        form.setIncomingTradePercentage(new BigDecimal("100.00"));
        return form;
    }

    private TradeOutgoingItemForm outgoingForm(Long ownedItemId, String overrideValue) {
        TradeOutgoingItemForm form = new TradeOutgoingItemForm();
        form.setOwnedItemId(ownedItemId);
        if (overrideValue != null) {
            form.setOverrideValueSgd(new BigDecimal(overrideValue));
        }
        return form;
    }

    private TradeIncomingItemForm incomingForm(Long cardId, String overrideValue) {
        TradeIncomingItemForm form = new TradeIncomingItemForm();
        form.setCardId(cardId);
        form.setVariant(CardVariant.STANDARD);
        form.setCondition(CardCondition.RAW_NEAR_MINT);
        form.setOverrideValueSgd(new BigDecimal(overrideValue));
        return form;
    }

    private TradeIncomingItemForm incomingSealedForm(Long sealedProductId, String overrideValue) {
        TradeIncomingItemForm form = new TradeIncomingItemForm();
        form.setSealedProductId(sealedProductId);
        form.setSealedCondition(SealedProductCondition.SEALED);
        form.setOverrideValueSgd(new BigDecimal(overrideValue));
        return form;
    }

    private OwnedItemDisposalForm saleForm(String salePrice, String fees) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setSalePriceSgd(new BigDecimal(salePrice));
        form.setFeesSgd(new BigDecimal(fees));
        return form;
    }

    private OwnedItemDisposalForm tradeForm(String tradeValue) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setTradeValueSgd(new BigDecimal(tradeValue));
        return form;
    }

    private OwnedItemDisposalForm deleteForm(String notes) {
        OwnedItemDisposalForm form = new OwnedItemDisposalForm();
        form.setDisposalDate(LocalDate.of(2026, 6, 4));
        form.setNotes(notes);
        return form;
    }
}
