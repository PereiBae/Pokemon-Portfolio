package com.pokemonportfolio.catalog.repository;

import com.pokemonportfolio.catalog.entity.SealedProduct;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SealedProductRepository extends JpaRepository<SealedProduct, Long> {

    List<SealedProduct> findByActiveTrueOrderByNameAsc();
}
