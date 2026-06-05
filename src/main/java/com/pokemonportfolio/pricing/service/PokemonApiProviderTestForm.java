package com.pokemonportfolio.pricing.service;

public class PokemonApiProviderTestForm {

    private PokemonApiProviderTestLookupType lookupType = PokemonApiProviderTestLookupType.CARD;
    private String searchOrId = "3852";

    public PokemonApiProviderTestLookupType getLookupType() {
        return lookupType;
    }

    public void setLookupType(PokemonApiProviderTestLookupType lookupType) {
        this.lookupType = lookupType;
    }

    public String getSearchOrId() {
        return searchOrId;
    }

    public void setSearchOrId(String searchOrId) {
        this.searchOrId = searchOrId;
    }
}
