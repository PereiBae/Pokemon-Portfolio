package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.catalog.service.CardService;
import com.pokemonportfolio.catalog.service.SealedProductService;
import com.pokemonportfolio.config.domain.CardVariant;
import com.pokemonportfolio.config.domain.GradedStatus;
import com.pokemonportfolio.config.domain.OwnedItemStatus;
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
    private final SealedProductService sealedProductService;

    public OwnedItemService(
            OwnedItemRepository ownedItemRepository,
            CardService cardService,
            SealedProductService sealedProductService) {
        this.ownedItemRepository = ownedItemRepository;
        this.cardService = cardService;
        this.sealedProductService = sealedProductService;
    }

    @Transactional
    public OwnedItem addCardToPortfolio(AppUser owner, OwnedItemForm form) {
        if (form.getCardId() == null) {
            throw new IllegalArgumentException("Card is required");
        }
        if (form.getCondition() == null) {
            throw new IllegalArgumentException("Card condition is required");
        }
        if (form.getPurchasePriceSgd() == null) {
            throw new IllegalArgumentException("Purchase price is required");
        }
        if (form.getPurchaseDate() == null) {
            throw new IllegalArgumentException("Purchase date is required");
        }
        if (form.getGradedStatus() == null) {
            throw new IllegalArgumentException("Graded status is required");
        }
        Card card = cardService.requireCard(form.getCardId());
        validateGradingFields(form);
        OwnedItem item = new OwnedItem(
                owner,
                card,
                selectedVariant(card, form),
                form.getCondition(),
                MoneyCalculationSupport.money(form.getPurchasePriceSgd()),
                form.getPurchaseDate(),
                form.getGradedStatus(),
                form.getPsaGrade(),
                blankToNull(form.getPsaCertificationNumber()),
                blankToNull(form.getNotes()));
        return ownedItemRepository.save(item);
    }

    @Transactional
    public OwnedItem addSealedProductToPortfolio(AppUser owner, OwnedItemForm form) {
        if (form.getSealedProductId() == null) {
            throw new IllegalArgumentException("Sealed product is required");
        }
        if (form.getSealedCondition() == null) {
            throw new IllegalArgumentException("Sealed product condition is required");
        }
        if (form.getPurchasePriceSgd() == null) {
            throw new IllegalArgumentException("Purchase price is required");
        }
        if (form.getPurchaseDate() == null) {
            throw new IllegalArgumentException("Purchase date is required");
        }
        SealedProduct sealedProduct = sealedProductService.requireSealedProduct(form.getSealedProductId());
        OwnedItem item = new OwnedItem(
                owner,
                sealedProduct,
                form.getSealedCondition(),
                MoneyCalculationSupport.money(form.getPurchasePriceSgd()),
                form.getPurchaseDate(),
                blankToNull(form.getNotes()));
        return ownedItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<OwnedItem> listActiveItems(AppUser owner) {
        return ownedItemRepository.findByOwnerAndStatusOrderByCreatedAtDesc(owner, OwnedItemStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<OwnedItem> listItems(AppUser owner) {
        return ownedItemRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    @Transactional(readOnly = true)
    public List<OwnedItemOptionView> listActiveItemOptions(AppUser owner) {
        return listActiveItems(owner).stream()
                .map(OwnedItemOptionView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnedItem requireActiveItemForOwner(AppUser owner, Long ownedItemId) {
        return ownedItemRepository.findByIdAndOwnerAndStatus(ownedItemId, owner, OwnedItemStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio item not found"));
    }

    @Transactional(readOnly = true)
    public OwnedItem requireItemForOwner(AppUser owner, Long ownedItemId) {
        return ownedItemRepository.findByIdAndOwner(ownedItemId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio item not found"));
    }

    private void validateGradingFields(OwnedItemForm form) {
        if (form.getGradedStatus() == GradedStatus.UNGRADED && form.getPsaGrade() != null) {
            throw new IllegalArgumentException("PSA grade can only be set for PSA graded cards");
        }
        if (form.getPsaGrade() != null && (form.getPsaGrade() < 1 || form.getPsaGrade() > 10)) {
            throw new IllegalArgumentException("PSA grade must be between 1 and 10");
        }
    }

    private CardVariant selectedVariant(Card card, OwnedItemForm form) {
        if (form.getVariant() != null) {
            return form.getVariant();
        }
        return card.getDefaultOwnedVariant();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
