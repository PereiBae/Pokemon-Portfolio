package com.pokemonportfolio.catalog.service;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.PokemonSet;
import com.pokemonportfolio.catalog.repository.CardRepository;
import com.pokemonportfolio.catalog.repository.PokemonSetRepository;
import com.pokemonportfolio.config.domain.LanguageMarket;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final PokemonSetRepository pokemonSetRepository;

    public CardService(CardRepository cardRepository, PokemonSetRepository pokemonSetRepository) {
        this.cardRepository = cardRepository;
        this.pokemonSetRepository = pokemonSetRepository;
    }

    @Transactional
    public Card createManualCard(CardForm form) {
        if (form.getLanguageMarket() != LanguageMarket.ENGLISH) {
            throw new IllegalArgumentException("Vertical Slice 1 supports English cards only");
        }
        PokemonSet pokemonSet = pokemonSetRepository
                .findByNameIgnoreCaseAndLanguageMarket(form.getSetName().trim(), LanguageMarket.ENGLISH)
                .orElseGet(() -> pokemonSetRepository.save(new PokemonSet(form.getSetName().trim(), LanguageMarket.ENGLISH)));

        Card card = new Card(
                pokemonSet,
                form.getName().trim(),
                form.getCardNumber().trim(),
                LanguageMarket.ENGLISH,
                form.getVariant());
        return cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public Card requireCard(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
    }

    @Transactional(readOnly = true)
    public List<Card> listEnglishCards(String query) {
        if (query == null || query.isBlank()) {
            return cardRepository.findByLanguageMarketAndActiveTrueOrderByNameAsc(LanguageMarket.ENGLISH);
        }
        return cardRepository.findByLanguageMarketAndNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(
                LanguageMarket.ENGLISH,
                query.trim());
    }

    @Transactional(readOnly = true)
    public List<CardOptionView> listEnglishCardOptions() {
        return cardRepository.findActiveByLanguageMarketWithSet(LanguageMarket.ENGLISH)
                .stream()
                .map(CardOptionView::from)
                .toList();
    }
}
