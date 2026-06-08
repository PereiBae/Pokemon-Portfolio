package com.pokemonportfolio.pricing.controller;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.service.CurrentUserService;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.SealedProductService;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.pricing.service.ManualPriceEntryForm;
import com.pokemonportfolio.pricing.service.ManualPriceEntryService;
import com.pokemonportfolio.pricing.service.MarketValuationService;
import com.pokemonportfolio.pricing.service.PriceRefreshSummaryView;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PricingController {

    private final CurrentUserService currentUserService;
    private final CardService cardService;
    private final SealedProductService sealedProductService;
    private final OwnedItemService ownedItemService;
    private final ManualPriceEntryService manualPriceEntryService;
    private final MarketValuationService marketValuationService;

    public PricingController(
            CurrentUserService currentUserService,
            CardService cardService,
            SealedProductService sealedProductService,
            OwnedItemService ownedItemService,
            ManualPriceEntryService manualPriceEntryService,
            MarketValuationService marketValuationService) {
        this.currentUserService = currentUserService;
        this.cardService = cardService;
        this.sealedProductService = sealedProductService;
        this.ownedItemService = ownedItemService;
        this.manualPriceEntryService = manualPriceEntryService;
        this.marketValuationService = marketValuationService;
    }

    @GetMapping("/pricing/manual-entry")
    String manualEntry(
            Authentication authentication,
            @RequestParam(name = "cardId", required = false) Long cardId,
            @RequestParam(name = "sealedProductId", required = false) Long sealedProductId,
            Model model) {
        ManualPriceEntryForm form = new ManualPriceEntryForm();
        form.setCardId(cardId);
        form.setSealedProductId(sealedProductId);
        form.setExchangeRateUsed(BigDecimal.ONE.setScale(8));
        prepareManualEntryModel(authentication, model, form);
        return "pricing/manual-entry";
    }

    @PostMapping("/pricing/refresh-real-prices")
    String refreshRealPrices(Authentication authentication, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        PriceRefreshSummaryView summary = marketValuationService.refreshRealPrices(owner);
        model.addAttribute("summary", summary);
        return "pricing/refresh-result";
    }

    @PostMapping("/pricing/manual-entry")
    String createManualEntry(
            Authentication authentication,
            @Valid @ModelAttribute("manualPriceEntryForm") ManualPriceEntryForm form,
            BindingResult bindingResult,
            Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                AppUser owner = currentUserService.requireCurrentUser(authentication);
                manualPriceEntryService.createManualSnapshot(owner, form);
                return "redirect:/pricing/manual-entry?success";
            } catch (IllegalArgumentException | IllegalStateException ex) {
                bindingResult.reject("manualPriceEntry.invalid", ex.getMessage());
            }
        }
        prepareManualEntryModel(authentication, model, form);
        return "pricing/manual-entry";
    }

    private void prepareManualEntryModel(Authentication authentication, Model model, ManualPriceEntryForm form) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("manualPriceEntryForm", form);
        model.addAttribute("cardOptions", cardService.listEnglishCardOptions());
        model.addAttribute("sealedProductOptions", sealedProductService.listActiveOptions());
        model.addAttribute("ownedItemOptions", ownedItemService.listActiveItemOptions(owner));
        model.addAttribute("confidenceRatings", ConfidenceRating.values());
        model.addAttribute("currencies", List.of("SGD", "USD", "EUR", "JPY", "CNY"));
    }
}
