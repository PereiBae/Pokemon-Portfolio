package com.pokemonportfolio.config.controller;

import com.pokemonportfolio.config.service.ProviderSettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SettingsController {

    private final ProviderSettingsService providerSettingsService;

    public SettingsController(ProviderSettingsService providerSettingsService) {
        this.providerSettingsService = providerSettingsService;
    }

    @GetMapping("/settings/providers")
    String providers(Model model) {
        model.addAttribute("providers", providerSettingsService.providerSettings());
        return "settings/providers";
    }

    @PostMapping("/settings/providers")
    String updateProvider(
            @RequestParam("providerKey") String providerKey,
            @RequestParam("enabled") boolean enabled) {
        providerSettingsService.setEnabled(providerKey, enabled);
        return "redirect:/settings/providers";
    }
}
