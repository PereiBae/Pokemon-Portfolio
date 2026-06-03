package com.pokemonportfolio.catalog.service;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.PokemonSet;
import com.pokemonportfolio.catalog.repository.CardRepository;
import com.pokemonportfolio.catalog.repository.PokemonSetRepository;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.CatalogSource;
import com.pokemonportfolio.config.domain.LanguageMarket;
import java.time.OffsetDateTime;
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

    @Transactional
    public Card importOfficialCard(OfficialCardSearchResult result) {
        if (result.getLanguageMarket() != LanguageMarket.ENGLISH) {
            throw new IllegalArgumentException("Only English official cards are supported in this phase");
        }
        if (result.getSource() != CatalogSource.POKEMON_TCG_API) {
            throw new IllegalArgumentException("Unsupported official catalogue source");
        }
        PokemonSet pokemonSet = findOrCreateOfficialSet(result);
        return cardRepository
                .findByCatalogSourceAndExternalCardId(result.getSource(), result.getExternalCardId())
                .map(existing -> updateVerifiedMetadata(existing, result, pokemonSet))
                .orElseGet(() -> importOrPromoteByIdentity(result, pokemonSet));
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

    private Card importOrPromoteByIdentity(OfficialCardSearchResult result, PokemonSet pokemonSet) {
        return cardRepository
                .findByIdentity(
                        result.getName().trim(),
                        result.getSetName().trim(),
                        result.getCardNumber().trim(),
                        LanguageMarket.ENGLISH,
                        CardVariant.STANDARD)
                .map(existing -> updateVerifiedMetadata(existing, result, pokemonSet))
                .orElseGet(() -> createVerifiedCard(result, pokemonSet));
    }

    private Card createVerifiedCard(OfficialCardSearchResult result, PokemonSet pokemonSet) {
        Card card = new Card(
                pokemonSet,
                result.getName().trim(),
                result.getCardNumber().trim(),
                LanguageMarket.ENGLISH,
                CardVariant.STANDARD);
        updateVerifiedMetadata(card, result, pokemonSet);
        return cardRepository.save(card);
    }

    private Card updateVerifiedMetadata(Card card, OfficialCardSearchResult result, PokemonSet pokemonSet) {
        card.moveToSet(pokemonSet);
        card.markVerified(
                result.getSource(),
                result.getExternalCardId(),
                result.getImageSmallUrl(),
                result.getImageLargeUrl(),
                result.getExternalCardUrl(),
                result.getRarity(),
                result.getAvailableVariants(),
                OffsetDateTime.now());
        return cardRepository.save(card);
    }

    private PokemonSet findOrCreateOfficialSet(OfficialCardSearchResult result) {
        String externalSetId = blankToNull(result.getExternalSetId());
        if (externalSetId != null) {
            return pokemonSetRepository
                    .findByExternalSetIdAndLanguageMarket(externalSetId, LanguageMarket.ENGLISH)
                    .map(existing -> updateOfficialSet(existing, result))
                    .orElseGet(() -> findByNameOrCreateOfficialSet(result));
        }
        return findByNameOrCreateOfficialSet(result);
    }

    private PokemonSet findByNameOrCreateOfficialSet(OfficialCardSearchResult result) {
        return pokemonSetRepository
                .findByNameIgnoreCaseAndLanguageMarket(result.getSetName().trim(), LanguageMarket.ENGLISH)
                .map(existing -> updateOfficialSet(existing, result))
                .orElseGet(() -> updateOfficialSet(new PokemonSet(result.getSetName().trim(), LanguageMarket.ENGLISH), result));
    }

    private PokemonSet updateOfficialSet(PokemonSet pokemonSet, OfficialCardSearchResult result) {
        pokemonSet.markOfficial(
                blankToNull(result.getExternalSetId()),
                blankToNull(result.getSetSeries()),
                result.getSetReleaseDate(),
                OffsetDateTime.now());
        return pokemonSetRepository.save(pokemonSet);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
