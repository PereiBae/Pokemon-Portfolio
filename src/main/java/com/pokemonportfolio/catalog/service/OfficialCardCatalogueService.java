package com.pokemonportfolio.catalog.service;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.provider.CardCatalogueProvider;
import com.pokemonportfolio.catalog.provider.CardCatalogueProviderException;
import com.pokemonportfolio.config.domain.CatalogSource;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficialCardCatalogueService {

    private final List<CardCatalogueProvider> providers;
    private final CardService cardService;

    public OfficialCardCatalogueService(List<CardCatalogueProvider> providers, CardService cardService) {
        this.providers = providers;
        this.cardService = cardService;
    }

    @Transactional(readOnly = true)
    public List<OfficialCardSearchResult> searchOfficialCards(String query) {
        return searchOfficialCards(OfficialCardSearchRequest.keyword(query)).getResults();
    }

    @Transactional(readOnly = true)
    public OfficialCardSearchPage searchOfficialCards(OfficialCardSearchRequest request) {
        if (request == null || !request.hasCriteria()) {
            return request == null
                    ? new OfficialCardSearchPage(List.of(), 1, OfficialCardSearchRequest.DEFAULT_PAGE_SIZE, null)
                    : OfficialCardSearchPage.empty(request);
        }
        CardCatalogueProvider provider = requireEnabledProvider(CatalogSource.POKEMON_TCG_API);
        return provider.search(request);
    }

    @Transactional
    public Card importOfficialCard(CatalogSource source, String externalCardId) {
        if (externalCardId == null || externalCardId.isBlank()) {
            throw new IllegalArgumentException("External card id is required");
        }
        CardCatalogueProvider provider = requireEnabledProvider(source);
        OfficialCardSearchResult result = provider.findByExternalId(externalCardId.trim());
        return cardService.importOfficialCard(result);
    }

    private CardCatalogueProvider requireEnabledProvider(CatalogSource source) {
        return providers.stream()
                .sorted(Comparator.comparing(provider -> provider.source().name()))
                .filter(provider -> provider.source() == source)
                .filter(CardCatalogueProvider::isEnabled)
                .findFirst()
                .orElseThrow(() -> new CardCatalogueProviderException("Official catalogue provider is not available."));
    }
}
