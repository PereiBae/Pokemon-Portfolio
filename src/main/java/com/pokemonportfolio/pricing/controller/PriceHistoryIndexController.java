package com.pokemonportfolio.pricing.controller;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.service.CurrentUserService;
import com.pokemonportfolio.portfolio.service.PortfolioDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PriceHistoryIndexController {

    private final CurrentUserService currentUserService;
    private final PortfolioDashboardService dashboardService;

    public PriceHistoryIndexController(
            CurrentUserService currentUserService,
            PortfolioDashboardService dashboardService) {
        this.currentUserService = currentUserService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/pricing/history")
    String priceHistoryIndex(Authentication authentication, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("summary", dashboardService.dashboardFor(owner));
        return "pricing/history-index";
    }
}
