package com.pokemonportfolio.pricing.controller;

import com.pokemonportfolio.pricing.service.PokeTraceProviderTestService;
import com.pokemonportfolio.pricing.service.PricingProviderProbeException;
import com.pokemonportfolio.pricing.service.PricingProviderProbeForm;
import com.pokemonportfolio.pricing.service.PricingProviderProbeResultView;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PokeTraceProviderTestController {

    private final PokeTraceProviderTestService testService;

    public PokeTraceProviderTestController(PokeTraceProviderTestService testService) {
        this.testService = testService;
    }

    @GetMapping("/settings/providers/poketrace/test")
    String testPage(Model model) {
        prepareModel(model, new PricingProviderProbeForm(), null, null);
        return "settings/poketrace-test";
    }

    @PostMapping("/settings/providers/poketrace/test")
    String runTest(
            @ModelAttribute("testForm") PricingProviderProbeForm form,
            Model model) {
        try {
            PricingProviderProbeResultView result = testService.test(form);
            prepareModel(model, form, result, null);
        } catch (PricingProviderProbeException | IllegalArgumentException ex) {
            prepareModel(model, form, null, ex.getMessage());
        }
        return "settings/poketrace-test";
    }

    private void prepareModel(
            Model model,
            PricingProviderProbeForm form,
            PricingProviderProbeResultView result,
            String errorMessage) {
        model.addAttribute("testForm", form);
        model.addAttribute("providerStatus", testService.status());
        model.addAttribute("result", result);
        model.addAttribute("errorMessage", errorMessage);
    }
}
