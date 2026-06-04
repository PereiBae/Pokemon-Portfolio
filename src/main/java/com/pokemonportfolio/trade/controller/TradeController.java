package com.pokemonportfolio.trade.controller;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.service.CurrentUserService;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.SealedProductService;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.SealedProductCondition;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.trade.service.TradeAnalyzerService;
import com.pokemonportfolio.trade.service.TradeCreateForm;
import com.pokemonportfolio.trade.service.TradeExecutionService;
import com.pokemonportfolio.trade.service.TradeIncomingItemForm;
import com.pokemonportfolio.trade.service.TradeOutgoingItemForm;
import com.pokemonportfolio.trade.service.TradePercentageForm;
import com.pokemonportfolio.trade.service.TradeTransactionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TradeController {

    private final CurrentUserService currentUserService;
    private final TradeTransactionService tradeTransactionService;
    private final TradeAnalyzerService tradeAnalyzerService;
    private final TradeExecutionService tradeExecutionService;
    private final OwnedItemService ownedItemService;
    private final CardService cardService;
    private final SealedProductService sealedProductService;

    public TradeController(
            CurrentUserService currentUserService,
            TradeTransactionService tradeTransactionService,
            TradeAnalyzerService tradeAnalyzerService,
            TradeExecutionService tradeExecutionService,
            OwnedItemService ownedItemService,
            CardService cardService,
            SealedProductService sealedProductService) {
        this.currentUserService = currentUserService;
        this.tradeTransactionService = tradeTransactionService;
        this.tradeAnalyzerService = tradeAnalyzerService;
        this.tradeExecutionService = tradeExecutionService;
        this.ownedItemService = ownedItemService;
        this.cardService = cardService;
        this.sealedProductService = sealedProductService;
    }

    @GetMapping("/trades")
    String index(Authentication authentication, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("trades", tradeTransactionService.list(owner));
        return "trade/index";
    }

    @GetMapping("/trades/new")
    String createForm(Model model) {
        model.addAttribute("tradeCreateForm", new TradeCreateForm());
        return "trade/new";
    }

    @PostMapping("/trades")
    String create(
            Authentication authentication,
            @Valid @ModelAttribute("tradeCreateForm") TradeCreateForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "trade/new";
        }
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        try {
            Long tradeTransactionId = tradeTransactionService.createDraft(owner, form).getId();
            return "redirect:/trades/" + tradeTransactionId;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "trade/new";
        }
    }

    @GetMapping("/trades/{tradeTransactionId}")
    String detail(
            Authentication authentication,
            @PathVariable Long tradeTransactionId,
            @RequestParam(name = "incomingCardId", required = false) Long incomingCardId,
            @RequestParam(name = "variant", required = false) CardVariant variant,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        prepareDetailModel(owner, tradeTransactionId, model, incomingCardId, variant);
        return "trade/detail";
    }

    @PostMapping("/trades/{tradeTransactionId}/percentages")
    String updatePercentages(
            Authentication authentication,
            @PathVariable Long tradeTransactionId,
            @ModelAttribute("percentageForm") TradePercentageForm form,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        try {
            tradeTransactionService.updatePercentages(owner, tradeTransactionId, form);
            tradeAnalyzerService.analyze(owner, tradeTransactionId);
            return "redirect:/trades/" + tradeTransactionId;
        } catch (IllegalArgumentException ex) {
            return detailWithError(owner, tradeTransactionId, model, ex.getMessage());
        }
    }

    @PostMapping("/trades/{tradeTransactionId}/outgoing")
    String addOutgoing(
            Authentication authentication,
            @PathVariable Long tradeTransactionId,
            @ModelAttribute("outgoingForm") TradeOutgoingItemForm form,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        try {
            tradeTransactionService.addOutgoingItem(owner, tradeTransactionId, form);
            return "redirect:/trades/" + tradeTransactionId;
        } catch (IllegalArgumentException ex) {
            return detailWithError(owner, tradeTransactionId, model, ex.getMessage());
        }
    }

    @PostMapping("/trades/{tradeTransactionId}/incoming")
    String addIncoming(
            Authentication authentication,
            @PathVariable Long tradeTransactionId,
            @ModelAttribute("incomingForm") TradeIncomingItemForm form,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        try {
            tradeTransactionService.addIncomingItem(owner, tradeTransactionId, form);
            return "redirect:/trades/" + tradeTransactionId;
        } catch (IllegalArgumentException ex) {
            return detailWithIncomingError(owner, tradeTransactionId, model, ex.getMessage(), form);
        }
    }

    @PostMapping("/trades/{tradeTransactionId}/analyze")
    String analyze(Authentication authentication, @PathVariable Long tradeTransactionId, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        try {
            tradeAnalyzerService.analyze(owner, tradeTransactionId);
            return "redirect:/trades/" + tradeTransactionId;
        } catch (IllegalArgumentException ex) {
            return detailWithError(owner, tradeTransactionId, model, ex.getMessage());
        }
    }

    @PostMapping("/trades/{tradeTransactionId}/execute")
    String execute(Authentication authentication, @PathVariable Long tradeTransactionId, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        try {
            tradeExecutionService.execute(owner, tradeTransactionId);
            return "redirect:/trades/" + tradeTransactionId;
        } catch (IllegalArgumentException ex) {
            return detailWithError(owner, tradeTransactionId, model, ex.getMessage());
        }
    }

    private String detailWithError(AppUser owner, Long tradeTransactionId, Model model, String errorMessage) {
        prepareDetailModel(owner, tradeTransactionId, model, null, null);
        model.addAttribute("errorMessage", errorMessage);
        return "trade/detail";
    }

    private String detailWithIncomingError(
            AppUser owner,
            Long tradeTransactionId,
            Model model,
            String errorMessage,
            TradeIncomingItemForm incomingForm) {
        prepareDetailModel(
                owner,
                tradeTransactionId,
                model,
                incomingForm.getCardId(),
                incomingForm.getVariant());
        model.addAttribute("incomingForm", incomingForm);
        model.addAttribute("errorMessage", errorMessage);
        return "trade/detail";
    }

    private void prepareDetailModel(
            AppUser owner,
            Long tradeTransactionId,
            Model model,
            Long incomingCardId,
            CardVariant variant) {
        var trade = tradeTransactionService.detailView(owner, tradeTransactionId);
        model.addAttribute("trade", trade);
        model.addAttribute("outgoingForm", new TradeOutgoingItemForm());
        TradeIncomingItemForm incomingForm = new TradeIncomingItemForm();
        incomingForm.setCardId(incomingCardId);
        if (variant != null) {
            incomingForm.setVariant(variant);
        }
        model.addAttribute("incomingForm", incomingForm);
        model.addAttribute("selectedIncomingCard", cardService.findCardOption(incomingCardId).orElse(null));
        TradePercentageForm percentageForm = new TradePercentageForm();
        percentageForm.setOutgoingTradePercentage(trade.outgoingTradePercentage());
        percentageForm.setIncomingTradePercentage(trade.incomingTradePercentage());
        model.addAttribute("percentageForm", percentageForm);
        model.addAttribute("activeItems", ownedItemService.listActiveItemOptions(owner));
        model.addAttribute("sealedProductOptions", sealedProductService.listActiveOptions());
        model.addAttribute("variants", CardVariant.values());
        model.addAttribute("conditions", CardCondition.values());
        model.addAttribute("sealedConditions", SealedProductCondition.values());
    }
}
