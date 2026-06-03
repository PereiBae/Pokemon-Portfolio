package com.pokemonportfolio.pricing.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.ConfidenceRating;
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
    private final OwnedItemService ownedItemService;
    private final CurrencyConversionService currencyConversionService;
    private final PriceSnapshotService priceSnapshotService;
    private final PricingProviderProperties pricingProviderProperties;

    public ManualPriceEntryService(
            CardService cardService,
            OwnedItemService ownedItemService,
            CurrencyConversionService currencyConversionService,
            PriceSnapshotService priceSnapshotService,
            PricingProviderProperties pricingProviderProperties) {
        this.cardService = cardService;
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
        Card card = resolveCard(owner, form);
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
        return priceSnapshotService.createSnapshot(card, providerPrice);
    }

    private Card resolveCard(AppUser owner, ManualPriceEntryForm form) {
        if (form.getOwnedItemId() != null) {
            return ownedItemService.requireActiveItemForOwner(owner, form.getOwnedItemId()).getCard();
        }
        if (form.getCardId() == null) {
            throw new IllegalArgumentException("Select a card or portfolio item");
        }
        return cardService.requireCard(form.getCardId());
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
}
