package com.pokemonportfolio.catalog.controller;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.LanguageMarket;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/cards/new")
    String newCard(Model model) {
        prepareForm(model, new CardForm());
        return "catalog/add-card";
    }

    @PostMapping("/cards")
    String createCard(@Valid @ModelAttribute("cardForm") CardForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form);
            return "catalog/add-card";
        }
        Card card = cardService.createManualCard(form);
        return "redirect:/portfolio/add?cardId=" + card.getId();
    }

    private void prepareForm(Model model, CardForm form) {
        model.addAttribute("cardForm", form);
        model.addAttribute("markets", LanguageMarket.values());
        model.addAttribute("variants", CardVariant.values());
    }
}

