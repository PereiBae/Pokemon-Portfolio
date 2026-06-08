package com.pokemonportfolio.config.controller;

import com.pokemonportfolio.config.service.ProviderSettingsService;
import com.pokemonportfolio.pricing.service.ExchangeRateForm;
import com.pokemonportfolio.pricing.service.ExchangeRateRefreshService;
import com.pokemonportfolio.pricing.service.ExchangeRateRefreshSummaryView;
import com.pokemonportfolio.pricing.service.ExchangeRateSettingsService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SettingsController {

    private final ProviderSettingsService providerSettingsService;
    private final ExchangeRateSettingsService exchangeRateSettingsService;
    private final ExchangeRateRefreshService exchangeRateRefreshService;

    public SettingsController(
            ProviderSettingsService providerSettingsService,
            ExchangeRateSettingsService exchangeRateSettingsService,
            ExchangeRateRefreshService exchangeRateRefreshService) {
        this.providerSettingsService = providerSettingsService;
        this.exchangeRateSettingsService = exchangeRateSettingsService;
        this.exchangeRateRefreshService = exchangeRateRefreshService;
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

    @GetMapping("/settings/exchange-rates")
    String exchangeRates(
            @RequestParam(name = "sourceCurrency", required = false) String sourceCurrency,
            Model model) {
        ExchangeRateForm form = new ExchangeRateForm();
        if (sourceCurrency != null && !sourceCurrency.isBlank()) {
            form.setSourceCurrency(sourceCurrency.toUpperCase());
        }
        prepareExchangeRateModel(model, form);
        return "settings/exchange-rates";
    }

    @PostMapping("/settings/exchange-rates")
    String recordExchangeRate(
            @Valid @ModelAttribute("exchangeRateForm") ExchangeRateForm form,
            BindingResult bindingResult,
            Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                exchangeRateSettingsService.recordManualRate(form);
                return "redirect:/settings/exchange-rates?success";
            } catch (IllegalArgumentException ex) {
                bindingResult.reject("exchangeRate.invalid", ex.getMessage());
            }
        }
        prepareExchangeRateModel(model, form);
        return "settings/exchange-rates";
    }

    @PostMapping("/settings/exchange-rates/refresh-defaults")
    String refreshDefaultExchangeRates(Model model) {
        ExchangeRateRefreshSummaryView summary = exchangeRateRefreshService.refreshDefaultSgdRates();
        model.addAttribute("refreshSummary", summary);
        prepareExchangeRateModel(model, new ExchangeRateForm());
        return "settings/exchange-rates";
    }

    private void prepareExchangeRateModel(Model model, ExchangeRateForm form) {
        model.addAttribute("exchangeRateForm", form);
        model.addAttribute("rates", exchangeRateSettingsService.listRates());
        model.addAttribute("sourceCurrencies", java.util.List.of("USD", "EUR", "JPY", "CNY", "SGD"));
    }
}
