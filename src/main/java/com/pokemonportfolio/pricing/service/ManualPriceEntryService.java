package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.SealedProductService;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.pricing.entity.PriceSnapshot;
import com.pokemonportfolio.pricing.provider.PricingProviderProperties;
import com.pokemonportfolio.pricing.provider.PricingProviderPrice;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualPriceEntryService {

    private final CardService cardService;
    private final SealedProductService sealedProductService;
    private final OwnedItemService ownedItemService;
    private final CurrencyConversionService currencyConversionService;
    private final PriceSnapshotService priceSnapshotService;
    private final PricingProviderProperties pricingProviderProperties;

    public ManualPriceEntryService(
            CardService cardService,
            SealedProductService sealedProductService,
            OwnedItemService ownedItemService,
            CurrencyConversionService currencyConversionService,
            PriceSnapshotService priceSnapshotService,
            PricingProviderProperties pricingProviderProperties) {
        this.cardService = cardService;
        this.sealedProductService = sealedProductService;
        this.ownedItemService = ownedItemService;
        this.currencyConversionService = currencyConversionService;
        this.priceSnapshotService = priceSnapshotService;
        this.pricingProviderProperties = pricingProviderProperties;
    }

    @Transactional
    public PriceSnapshot createManualSnapshot(AppUser owner, ManualPriceEntryForm form) {
        if (!pricingProviderProperties.isManualEntryEnabled()) {
            throw new IllegalStateException("Manual price entry is disabled");
        }
        ManualPriceAsset asset = resolveAsset(owner, form);
        ConfidenceRating confidenceRating = form.getConfidenceRating() == null
                ? ConfidenceRating.LOW
                : form.getConfidenceRating();
        String sourceCurrency = currencyConversionService.normalizeCurrency(form.getSourceCurrency());
        BigDecimal exchangeRate = currencyConversionService.normalizeRate(sourceCurrency, form.getExchangeRateUsed());
        BigDecimal calculatedSgd = currencyConversionService.convertToSgd(
                form.getSourcePrice(),
                sourceCurrency,
                exchangeRate);
        BigDecimal submittedSgd = MoneyCalculationSupport.money(form.getMarketPriceSgd());

        if (submittedSgd.compareTo(calculatedSgd) != 0) {
            throw new IllegalArgumentException("Calculated SGD value must match source price multiplied by exchange rate");
        }

        currencyConversionService.recordManualRate(sourceCurrency, exchangeRate, confidenceRating);
        PricingProviderPrice providerPrice = new PricingProviderPrice(
                providerLabel(form.getProviderName()),
                MoneyCalculationSupport.money(form.getSourcePrice()),
                sourceCurrency,
                exchangeRate,
                submittedSgd,
                confidenceRating,
                explanation(form.getNotes()));
        if (asset.card() != null) {
            if (asset.cardVariant() != null) {
                return priceSnapshotService.createSnapshot(asset.card(), asset.cardVariant(), providerPrice);
            }
            return priceSnapshotService.createSnapshot(asset.card(), providerPrice);
        }
        return priceSnapshotService.createSnapshot(asset.sealedProduct(), providerPrice);
    }

    private ManualPriceAsset resolveAsset(AppUser owner, ManualPriceEntryForm form) {
        if (form.getOwnedItemId() != null) {
            OwnedItem ownedItem = ownedItemService.requireActiveItemForOwner(owner, form.getOwnedItemId());
            return new ManualPriceAsset(ownedItem.getCard(), ownedItem.getOwnedVariant(), ownedItem.getSealedProduct());
        }
        if (form.getCardId() != null) {
            return new ManualPriceAsset(cardService.requireCard(form.getCardId()), null, null);
        }
        if (form.getSealedProductId() != null) {
            return new ManualPriceAsset(null, null, sealedProductService.requireSealedProduct(form.getSealedProductId()));
        }
        throw new IllegalArgumentException("Select a card, sealed product, or portfolio item");
    }

    private String providerLabel(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return "MANUAL";
        }
        return providerName.trim().toUpperCase();
    }

    private String explanation(String notes) {
        if (notes == null || notes.isBlank()) {
            return "Manual price entry fallback with auditable source currency and exchange-rate fields.";
        }
        return "Manual price entry fallback. Notes: " + notes.trim();
    }

    private record ManualPriceAsset(Card card, CardVariant cardVariant, SealedProduct sealedProduct) {
    }
}
