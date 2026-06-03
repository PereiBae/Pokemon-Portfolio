package com.pokemonportfolio.catalog.provider;

import com.pokemonportfolio.catalog.service.OfficialCardSearchResult;
import com.pokemonportfolio.catalog.service.OfficialCardSearchPage;
import com.pokemonportfolio.catalog.service.OfficialCardSearchRequest;
import com.pokemonportfolio.config.domain.CatalogSource;
import java.util.List;

public interface CardCatalogueProvider {

    CatalogSource source();

    boolean isEnabled();

    OfficialCardSearchPage search(OfficialCardSearchRequest request);

    default List<OfficialCardSearchResult> search(String query) {
        return search(OfficialCardSearchRequest.keyword(query)).getResults();
    }

    OfficialCardSearchResult findByExternalId(String externalCardId);
}
