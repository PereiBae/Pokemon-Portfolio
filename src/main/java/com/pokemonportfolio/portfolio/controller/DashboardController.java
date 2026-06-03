package com.pokemonportfolio.portfolio.controller;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.service.CurrentUserService;
import com.pokemonportfolio.portfolio.service.PortfolioDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final CurrentUserService currentUserService;
    private final PortfolioDashboardService dashboardService;

    public DashboardController(CurrentUserService currentUserService, PortfolioDashboardService dashboardService) {
        this.currentUserService = currentUserService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    String dashboard(Authentication authentication, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("owner", owner);
        model.addAttribute("summary", dashboardService.dashboardFor(owner));
        return "dashboard/index";
    }
}

