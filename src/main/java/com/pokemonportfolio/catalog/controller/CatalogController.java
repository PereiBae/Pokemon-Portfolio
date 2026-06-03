package com.pokemonportfolio.catalog.controller;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.provider.CardCatalogueProviderException;
import com.pokemonportfolio.catalog.service.OfficialCardCatalogueService;
import com.pokemonportfolio.catalog.service.OfficialCardSearchPage;
import com.pokemonportfolio.catalog.service.OfficialCardSearchRequest;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CatalogController {

    private final OfficialCardCatalogueService officialCardCatalogueService;

    public CatalogController(OfficialCardCatalogueService officialCardCatalogueService) {
        this.officialCardCatalogueService = officialCardCatalogueService;
    }

    @GetMapping("/catalog/search")
    String search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "cardName", required = false) String cardName,
            @RequestParam(name = "setName", required = false) String setName,
            @RequestParam(name = "cardNumber", required = false) String cardNumber,
            @RequestParam(name = "rarity", required = false) String rarity,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            Model model) {
        OfficialCardSearchRequest request =
                new OfficialCardSearchRequest(query, cardName, setName, cardNumber, rarity, page, pageSize);
        OfficialCardSearchPage resultsPage = OfficialCardSearchPage.empty(request);
        if (request.hasCriteria()) {
            try {
                resultsPage = officialCardCatalogueService.searchOfficialCards(request);
            } catch (CardCatalogueProviderException ex) {
                model.addAttribute("catalogError", friendlyError());
            }
        }
        model.addAttribute("searchRequest", request);
        model.addAttribute("resultsPage", resultsPage);
        model.addAttribute("results", resultsPage.getResults());
        model.addAttribute("pageSizeOptions", OfficialCardSearchRequest.PAGE_SIZE_OPTIONS);
        return "catalog/search";
    }

    @PostMapping("/catalog/import")
    String importCard(
            @RequestParam("source") CatalogSource source,
            @RequestParam("externalCardId") String externalCardId,
            @RequestParam(name = "action", defaultValue = "import") String action,
            @RequestParam(name = "variant", required = false) CardVariant variant,
            RedirectAttributes redirectAttributes) {
        try {
            Card card = officialCardCatalogueService.importOfficialCard(source, externalCardId);
            if ("portfolio".equalsIgnoreCase(action)) {
                String variantParam = variant == null ? "" : "&variant=" + variant.name();
                return "redirect:/portfolio/add?cardId=" + card.getId() + variantParam;
            }
            redirectAttributes.addFlashAttribute("successMessage", "Verified card imported.");
            return "redirect:/catalog/search";
        } catch (CardCatalogueProviderException ex) {
            redirectAttributes.addFlashAttribute("catalogError", friendlyError());
            return "redirect:/catalog/search";
        }
    }

    private String friendlyError() {
        return "Official catalogue is unavailable right now. Create a custom unverified card as a local fallback.";
    }
}
