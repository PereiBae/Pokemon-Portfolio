package com.pokemonportfolio.pricing.controller;

import com.pokemonportfolio.pricing.service.PokemonApiProviderTestException;
import com.pokemonportfolio.pricing.service.PokemonApiProviderTestForm;
import com.pokemonportfolio.pricing.service.PokemonApiProviderTestLookupType;
import com.pokemonportfolio.pricing.service.PokemonApiProviderTestResultView;
import com.pokemonportfolio.pricing.service.PokemonApiProviderTestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PokemonApiProviderTestController {

    private final PokemonApiProviderTestService testService;

    public PokemonApiProviderTestController(PokemonApiProviderTestService testService) {
        this.testService = testService;
    }

    @GetMapping("/settings/providers/pokemon-api/test")
    String testPage(Model model) {
        prepareModel(model, new PokemonApiProviderTestForm(), null, null);
        return "settings/pokemon-api-test";
    }

    @PostMapping("/settings/providers/pokemon-api/test")
    String runTest(
            @ModelAttribute("testForm") PokemonApiProviderTestForm form,
            Model model) {
        try {
            PokemonApiProviderTestResultView result = testService.test(form);
            prepareModel(model, form, result, null);
        } catch (PokemonApiProviderTestException | IllegalArgumentException ex) {
            prepareModel(model, form, null, ex.getMessage());
        }
        return "settings/pokemon-api-test";
    }

    private void prepareModel(
            Model model,
            PokemonApiProviderTestForm form,
            PokemonApiProviderTestResultView result,
            String errorMessage) {
        model.addAttribute("testForm", form);
        model.addAttribute("providerStatus", testService.status());
        model.addAttribute("lookupTypes", PokemonApiProviderTestLookupType.values());
        model.addAttribute("result", result);
        model.addAttribute("errorMessage", errorMessage);
    }
}
