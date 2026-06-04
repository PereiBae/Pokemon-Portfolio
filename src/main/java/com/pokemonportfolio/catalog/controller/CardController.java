package com.pokemonportfolio.catalog.controller;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardForm;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.LanguageMarket;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/cards/new")
    String newCard(@RequestParam(name = "returnTradeId", required = false) Long returnTradeId, Model model) {
        prepareForm(model, new CardForm(), returnTradeId);
        return "catalog/add-card";
    }

    @PostMapping("/cards")
    String createCard(
            @Valid @ModelAttribute("cardForm") CardForm form,
            BindingResult bindingResult,
            @RequestParam(name = "returnTradeId", required = false) Long returnTradeId,
            Model model) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, returnTradeId);
            return "catalog/add-card";
        }
        Card card = cardService.createManualCard(form);
        if (returnTradeId != null) {
            String variantParam = form.getVariant() == null ? "" : "&variant=" + form.getVariant().name();
            return "redirect:/trades/" + returnTradeId + "?incomingCardId=" + card.getId() + variantParam;
        }
        return "redirect:/portfolio/add?cardId=" + card.getId();
    }

    private void prepareForm(Model model, CardForm form, Long returnTradeId) {
        model.addAttribute("cardForm", form);
        model.addAttribute("markets", List.of(LanguageMarket.ENGLISH));
        model.addAttribute("variants", CardVariant.values());
        model.addAttribute("returnTradeId", returnTradeId);
    }
}
