package com.pokemonportfolio.pricing.controller;

import com.pokemonportfolio.pricing.service.PricingProviderComparisonForm;
import com.pokemonportfolio.pricing.service.PricingProviderComparisonPageView;
import com.pokemonportfolio.pricing.service.PricingProviderComparisonService;
import com.pokemonportfolio.pricing.service.PricingProviderProbeException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PricingProviderComparisonController {

    private final PricingProviderComparisonService comparisonService;

    public PricingProviderComparisonController(PricingProviderComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @GetMapping("/settings/providers/comparison/test")
    String comparisonPage(Model model) {
        prepareModel(model, new PricingProviderComparisonForm(), null, null);
        return "settings/pricing-provider-comparison";
    }

    @PostMapping("/settings/providers/comparison/test")
    String compare(
            @ModelAttribute("comparisonForm") PricingProviderComparisonForm form,
            Model model) {
        try {
            PricingProviderComparisonPageView comparison = comparisonService.compare(form);
            prepareModel(model, form, comparison, null);
        } catch (PricingProviderProbeException | IllegalArgumentException ex) {
            prepareModel(model, form, null, ex.getMessage());
        }
        return "settings/pricing-provider-comparison";
    }

    private void prepareModel(
            Model model,
            PricingProviderComparisonForm form,
            PricingProviderComparisonPageView comparison,
            String errorMessage) {
        model.addAttribute("comparisonForm", form);
        model.addAttribute("comparison", comparison);
        model.addAttribute("errorMessage", errorMessage);
    }
}
