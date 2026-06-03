package com.pokemonportfolio.portfolio.controller;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.service.CurrentUserService;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardCondition;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.portfolio.service.OwnedItemForm;
import com.pokemonportfolio.portfolio.service.PortfolioDashboardService;
import com.pokemonportfolio.portfolio.service.PortfolioWorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PortfolioController {

    private final CurrentUserService currentUserService;
    private final CardService cardService;
    private final PortfolioDashboardService dashboardService;
    private final PortfolioWorkflowService workflowService;

    public PortfolioController(
            CurrentUserService currentUserService,
            CardService cardService,
            PortfolioDashboardService dashboardService,
            PortfolioWorkflowService workflowService) {
        this.currentUserService = currentUserService;
        this.cardService = cardService;
        this.dashboardService = dashboardService;
        this.workflowService = workflowService;
    }

    @GetMapping("/portfolio")
    String portfolio(Authentication authentication, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("summary", dashboardService.dashboardFor(owner));
        return "portfolio/list";
    }

    @GetMapping("/portfolio/add")
    String addItem(@RequestParam(name = "cardId", required = false) Long cardId, Model model) {
        OwnedItemForm form = new OwnedItemForm();
        form.setCardId(cardId);
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

    private void prepareAddModel(Model model, OwnedItemForm form) {
        model.addAttribute("ownedItemForm", form);
        model.addAttribute("cardOptions", cardService.listEnglishCardOptions());
        model.addAttribute("conditions", CardCondition.values());
        model.addAttribute("gradedStatuses", GradedStatus.values());
    }
}
