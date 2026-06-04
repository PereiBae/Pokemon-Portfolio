package com.pokemonportfolio.catalog.controller;

import com.pokemonportfolio.catalog.service.SealedProductForm;
import com.pokemonportfolio.catalog.service.SealedProductService;
import com.pokemonportfolio.config.domain.LanguageMarket;
import com.pokemonportfolio.config.domain.SealedProductType;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SealedProductController {

    private final SealedProductService sealedProductService;

    public SealedProductController(SealedProductService sealedProductService) {
        this.sealedProductService = sealedProductService;
    }

    @GetMapping("/sealed-products")
    String index(Model model) {
        model.addAttribute("sealedProducts", sealedProductService.listActiveOptions());
        return "catalog/sealed-products";
    }

    @GetMapping("/sealed-products/new")
    String newSealedProduct(Model model) {
        prepareForm(model, new SealedProductForm());
        return "catalog/add-sealed-product";
    }

    @PostMapping("/sealed-products")
    String createSealedProduct(
            @Valid @ModelAttribute("sealedProductForm") SealedProductForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form);
            return "catalog/add-sealed-product";
        }
        var product = sealedProductService.createManualSealedProduct(form);
        return "redirect:/portfolio/add-sealed?sealedProductId=" + product.getId();
    }

    private void prepareForm(Model model, SealedProductForm form) {
        model.addAttribute("sealedProductForm", form);
        model.addAttribute("productTypes", SealedProductType.values());
        model.addAttribute("markets", LanguageMarket.values());
    }
}
