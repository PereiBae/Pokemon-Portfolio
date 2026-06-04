package com.pokemonportfolio.catalog.service;

import com.pokemonportfolio.catalog.entity.SealedProduct;
import com.pokemonportfolio.catalog.repository.SealedProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SealedProductService {

    private final SealedProductRepository sealedProductRepository;

    public SealedProductService(SealedProductRepository sealedProductRepository) {
        this.sealedProductRepository = sealedProductRepository;
    }

    @Transactional
    public SealedProduct createManualSealedProduct(SealedProductForm form) {
        SealedProduct product = new SealedProduct(
                requireText(form.getName(), "Product name is required"),
                form.getProductType(),
                form.getLanguageMarket(),
                blankToNull(form.getSetName()),
                form.getReleaseDate(),
                blankToNull(form.getImageUrl()),
                blankToNull(form.getNotes()));
        return sealedProductRepository.save(product);
    }

    @Transactional(readOnly = true)
    public SealedProduct requireSealedProduct(Long id) {
        return sealedProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sealed product not found"));
    }

    @Transactional(readOnly = true)
    public List<SealedProductOptionView> listActiveOptions() {
        return sealedProductRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(SealedProductOptionView::from)
                .toList();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
