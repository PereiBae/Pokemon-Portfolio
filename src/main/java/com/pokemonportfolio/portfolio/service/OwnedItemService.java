package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.portfolio.entity.OwnedItem;
import com.pokemonportfolio.portfolio.repository.OwnedItemRepository;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnedItemService {

    private final OwnedItemRepository ownedItemRepository;
    private final CardService cardService;

    public OwnedItemService(OwnedItemRepository ownedItemRepository, CardService cardService) {
        this.ownedItemRepository = ownedItemRepository;
        this.cardService = cardService;
    }

    @Transactional
    public OwnedItem addCardToPortfolio(AppUser owner, OwnedItemForm form) {
        Card card = cardService.requireCard(form.getCardId());
        validateGradingFields(form);
        OwnedItem item = new OwnedItem(
                owner,
                card,
                form.getCondition(),
                MoneyCalculationSupport.money(form.getPurchasePriceSgd()),
                form.getPurchaseDate(),
                form.getGradedStatus(),
                form.getPsaGrade(),
                blankToNull(form.getPsaCertificationNumber()),
                blankToNull(form.getNotes()));
        return ownedItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<OwnedItem> listActiveItems(AppUser owner) {
        return ownedItemRepository.findByOwnerAndArchivedAtIsNullOrderByCreatedAtDesc(owner);
    }

    private void validateGradingFields(OwnedItemForm form) {
        if (form.getGradedStatus() == GradedStatus.UNGRADED && form.getPsaGrade() != null) {
            throw new IllegalArgumentException("PSA grade can only be set for PSA graded cards");
        }
        if (form.getPsaGrade() != null && (form.getPsaGrade() < 1 || form.getPsaGrade() > 10)) {
            throw new IllegalArgumentException("PSA grade must be between 1 and 10");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

