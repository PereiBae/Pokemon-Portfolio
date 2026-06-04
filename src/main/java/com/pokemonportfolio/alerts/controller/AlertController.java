package com.pokemonportfolio.alerts.controller;

import com.pokemonportfolio.alerts.service.AlertViewService;
import com.pokemonportfolio.alerts.service.PriceAlertService;
import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AlertController {

    private final CurrentUserService currentUserService;
    private final PriceAlertService priceAlertService;
    private final AlertViewService alertViewService;

    public AlertController(
            CurrentUserService currentUserService,
            PriceAlertService priceAlertService,
            AlertViewService alertViewService) {
        this.currentUserService = currentUserService;
        this.priceAlertService = priceAlertService;
        this.alertViewService = alertViewService;
    }

    @GetMapping("/alerts")
    String alerts(Authentication authentication, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("alerts", alertViewService.pageFor(owner));
        return "alerts/index";
    }

    @PostMapping("/alerts/check")
    String checkAlerts(Authentication authentication, RedirectAttributes redirectAttributes) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        int createdCount = priceAlertService.checkAlerts(owner).size();
        redirectAttributes.addFlashAttribute("successMessage", createdCount + " new alert(s) created.");
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/{alertId}/dismiss")
    String dismissAlert(
            Authentication authentication,
            @PathVariable Long alertId,
            RedirectAttributes redirectAttributes) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        alertViewService.dismissAlert(owner, alertId);
        redirectAttributes.addFlashAttribute("successMessage", "Alert dismissed.");
        return "redirect:/alerts";
    }
}
