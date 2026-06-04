package com.pokemonportfolio.portfolio.controller;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.service.CurrentUserService;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.DisposalType;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.OwnedItemDisposalForm;
import com.pokemonportfolio.portfolio.service.OwnedItemOptionView;
import com.pokemonportfolio.portfolio.service.OwnedItemService;
import com.pokemonportfolio.portfolio.service.PortfolioDashboardService;
import com.pokemonportfolio.portfolio.service.PortfolioDisposalService;
import com.pokemonportfolio.portfolio.service.PortfolioWorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PortfolioController {

    private final CurrentUserService currentUserService;
    private final CardService cardService;
    private final OwnedItemService ownedItemService;
    private final PortfolioDashboardService dashboardService;
    private final PortfolioWorkflowService workflowService;
    private final PortfolioDisposalService disposalService;

    public PortfolioController(
            CurrentUserService currentUserService,
            CardService cardService,
            OwnedItemService ownedItemService,
            PortfolioDashboardService dashboardService,
            PortfolioWorkflowService workflowService,
            PortfolioDisposalService disposalService) {
        this.currentUserService = currentUserService;
        this.cardService = cardService;
        this.ownedItemService = ownedItemService;
        this.dashboardService = dashboardService;
        this.workflowService = workflowService;
        this.disposalService = disposalService;
    }

    @GetMapping("/portfolio")
    String portfolio(Authentication authentication, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("summary", dashboardService.dashboardFor(owner));
        return "portfolio/list";
    }

    @GetMapping("/portfolio/disposals")
    String disposalHistory(Authentication authentication, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("disposals", disposalService.listDisposalHistory(owner));
        return "portfolio/disposals";
    }

    @GetMapping("/portfolio/add")
    String addItem(
            @RequestParam(name = "cardId", required = false) Long cardId,
            @RequestParam(name = "variant", required = false) CardVariant variant,
            Model model) {
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(cardId);
        if (variant != null) {
            form.setVariant(variant);
        } else if (cardId != null) {
            form.setVariant(cardService.requireCard(cardId).getDefaultOwnedVariant());
        }
        prepareAddModel(model, form);
        return "portfolio/add";
    }

    @PostMapping("/portfolio/items")
    String createItem(
            Authentication authentication,
            @Valid @ModelAttribute("ownedItemForm") OwnedItemForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            prepareAddModel(model, form);
            return "portfolio/add";
        }
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        workflowService.addCardToPortfolioAndSnapshot(owner, form);
        return "redirect:/dashboard";
    }

    @GetMapping("/portfolio/items/{ownedItemId}/sell")
    String sellForm(Authentication authentication, @PathVariable Long ownedItemId, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        prepareDisposeModel(owner, ownedItemId, model, new OwnedItemDisposalForm(), DisposalType.SOLD);
        return "portfolio/dispose";
    }

    @PostMapping("/portfolio/items/{ownedItemId}/sell")
    String sellItem(
            Authentication authentication,
            @PathVariable Long ownedItemId,
            @Valid @ModelAttribute("disposalForm") OwnedItemDisposalForm form,
            BindingResult bindingResult,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        validateDisposalForm(form, bindingResult, DisposalType.SOLD);
        if (bindingResult.hasErrors()) {
            prepareDisposeModel(owner, ownedItemId, model, form, DisposalType.SOLD);
            return "portfolio/dispose";
        }
        disposalService.sellItem(owner, ownedItemId, form);
        return "redirect:/portfolio/disposals";
    }

    @GetMapping("/portfolio/items/{ownedItemId}/trade-away")
    String tradeAwayForm(Authentication authentication, @PathVariable Long ownedItemId, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        prepareDisposeModel(owner, ownedItemId, model, new OwnedItemDisposalForm(), DisposalType.TRADED);
        return "portfolio/dispose";
    }

    @PostMapping("/portfolio/items/{ownedItemId}/trade-away")
    String tradeAwayItem(
            Authentication authentication,
            @PathVariable Long ownedItemId,
            @Valid @ModelAttribute("disposalForm") OwnedItemDisposalForm form,
            BindingResult bindingResult,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        validateDisposalForm(form, bindingResult, DisposalType.TRADED);
        if (bindingResult.hasErrors()) {
            prepareDisposeModel(owner, ownedItemId, model, form, DisposalType.TRADED);
            return "portfolio/dispose";
        }
        disposalService.tradeAwayItem(owner, ownedItemId, form);
        return "redirect:/portfolio/disposals";
    }

    @GetMapping("/portfolio/items/{ownedItemId}/delete-mistake")
    String deleteMistakeForm(Authentication authentication, @PathVariable Long ownedItemId, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        prepareDisposeModel(owner, ownedItemId, model, new OwnedItemDisposalForm(), DisposalType.DELETED);
        return "portfolio/dispose";
    }

    @PostMapping("/portfolio/items/{ownedItemId}/delete-mistake")
    String deleteMistake(
            Authentication authentication,
            @PathVariable Long ownedItemId,
            @Valid @ModelAttribute("disposalForm") OwnedItemDisposalForm form,
            BindingResult bindingResult,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        if (bindingResult.hasErrors()) {
            prepareDisposeModel(owner, ownedItemId, model, form, DisposalType.DELETED);
            return "portfolio/dispose";
        }
        disposalService.deleteMistake(owner, ownedItemId, form);
        return "redirect:/portfolio/disposals";
    }

    private void prepareAddModel(Model model, OwnedItemForm form) {
        model.addAttribute("ownedItemForm", form);
        model.addAttribute("cardOptions", cardService.listEnglishCardOptions());
        model.addAttribute("variants", CardVariant.values());
        model.addAttribute("conditions", CardCondition.values());
        model.addAttribute("gradedStatuses", GradedStatus.values());
    }

    private void prepareDisposeModel(
            AppUser owner,
            Long ownedItemId,
            Model model,
            OwnedItemDisposalForm form,
            DisposalType disposalType) {
        var item = ownedItemService.requireActiveItemForOwner(owner, ownedItemId);
        OwnedItemOptionView itemView = OwnedItemOptionView.from(item);
        model.addAttribute("item", itemView);
        model.addAttribute("purchasePriceSgd", item.getPurchasePriceSgd());
        model.addAttribute("disposalForm", form);
        model.addAttribute("disposalType", disposalType);
        model.addAttribute("actionPath", actionPath(item.getId(), disposalType));
    }

    private String actionPath(Long ownedItemId, DisposalType disposalType) {
        return switch (disposalType) {
            case SOLD -> "/portfolio/items/" + ownedItemId + "/sell";
            case TRADED -> "/portfolio/items/" + ownedItemId + "/trade-away";
            case DELETED -> "/portfolio/items/" + ownedItemId + "/delete-mistake";
        };
    }

    private void validateDisposalForm(
            OwnedItemDisposalForm form,
            BindingResult bindingResult,
            DisposalType disposalType) {
        if (disposalType == DisposalType.SOLD && form.getSalePriceSgd() == null) {
            bindingResult.rejectValue("salePriceSgd", "required", "Sale price is required");
        }
        if (disposalType == DisposalType.TRADED && form.getTradeValueSgd() == null) {
            bindingResult.rejectValue("tradeValueSgd", "required", "Trade value received is required");
        }
    }
}
