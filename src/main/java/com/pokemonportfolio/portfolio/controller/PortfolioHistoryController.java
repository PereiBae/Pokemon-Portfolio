package com.pokemonportfolio.portfolio.controller;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.service.CurrentUserService;
import com.pokemonportfolio.portfolio.service.PortfolioHistoryService;
import com.pokemonportfolio.portfolio.service.PortfolioValuationService;
import com.pokemonportfolio.pricing.service.PriceHistoryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PortfolioHistoryController {

    private final CurrentUserService currentUserService;
    private final PortfolioHistoryService portfolioHistoryService;
    private final PortfolioValuationService portfolioValuationService;
    private final PriceHistoryService priceHistoryService;

    public PortfolioHistoryController(
            CurrentUserService currentUserService,
            PortfolioHistoryService portfolioHistoryService,
            PortfolioValuationService portfolioValuationService,
            PriceHistoryService priceHistoryService) {
        this.currentUserService = currentUserService;
        this.portfolioHistoryService = portfolioHistoryService;
        this.portfolioValuationService = portfolioValuationService;
        this.priceHistoryService = priceHistoryService;
    }

    @GetMapping("/portfolio/history")
    String portfolioHistory(
            Authentication authentication,
            @RequestParam(name = "range", required = false) String range,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("history", portfolioHistoryService.historyFor(owner, range));
        return "portfolio/history";
    }

    @PostMapping("/portfolio/history/snapshot")
    String createPortfolioSnapshot(Authentication authentication) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        portfolioValuationService.createSnapshot(owner);
        return "redirect:/portfolio/history?snapshotCreated";
    }

    @GetMapping("/portfolio/items/{ownedItemId}/history")
    String itemHistory(
            Authentication authentication,
            @PathVariable Long ownedItemId,
            @RequestParam(name = "range", required = false) String range,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("history", priceHistoryService.historyFor(owner, ownedItemId, range));
        return "pricing/item-history";
    }
}
