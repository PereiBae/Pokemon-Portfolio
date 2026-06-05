package com.pokemonportfolio.grading.controller;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.service.CurrentUserService;
import com.pokemonportfolio.grading.service.GradingAnalysisForm;
import com.pokemonportfolio.grading.service.GradingAnalyzerService;
import com.pokemonportfolio.grading.service.GradingFeeService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GradingController {

    private static final int LATEST_ANALYSIS_LIMIT = 5;

    private final CurrentUserService currentUserService;
    private final GradingAnalyzerService gradingAnalyzerService;
    private final GradingFeeService gradingFeeService;

    public GradingController(
            CurrentUserService currentUserService,
            GradingAnalyzerService gradingAnalyzerService,
            GradingFeeService gradingFeeService) {
        this.currentUserService = currentUserService;
        this.gradingAnalyzerService = gradingAnalyzerService;
        this.gradingFeeService = gradingFeeService;
    }

    @GetMapping("/grading")
    String index(
            Authentication authentication,
            @RequestParam(name = "ownedItemId", required = false) Long ownedItemId,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        try {
            prepareIndexModel(owner, model, gradingAnalyzerService.formFor(owner, ownedItemId));
        } catch (IllegalArgumentException ex) {
            prepareIndexModel(owner, model, gradingAnalyzerService.formFor(owner, null));
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "grading/index";
    }

    @PostMapping("/grading/analyze")
    String analyze(
            Authentication authentication,
            @ModelAttribute("gradingForm") GradingAnalysisForm form,
            BindingResult bindingResult,
            Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        if (!bindingResult.hasErrors()) {
            try {
                var analysis = gradingAnalyzerService.analyze(owner, form);
                return "redirect:/grading/" + analysis.id();
            } catch (IllegalArgumentException ex) {
                bindingResult.reject("grading.invalid", ex.getMessage());
            }
        }
        prepareIndexModel(owner, model, form);
        return "grading/index";
    }

    @GetMapping("/grading/{analysisId}")
    String detail(Authentication authentication, @PathVariable Long analysisId, Model model) {
        AppUser owner = currentUserService.requireCurrentUser(authentication);
        model.addAttribute("analysis", gradingAnalyzerService.view(owner, analysisId));
        return "grading/detail";
    }

    private void prepareIndexModel(AppUser owner, Model model, GradingAnalysisForm form) {
        model.addAttribute("gradingForm", form);
        model.addAttribute("eligibleItems", gradingAnalyzerService.eligibleItems(owner));
        model.addAttribute("feeOptions", gradingFeeService.listActiveFees());
        model.addAttribute("latestAnalyses", gradingAnalyzerService.latestAnalyses(owner, LATEST_ANALYSIS_LIMIT));
    }
}
