package com.pokemonportfolio.grading.repository;

import com.pokemonportfolio.config.domain.GradingCompany;
import com.pokemonportfolio.grading.entity.GradingFee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradingFeeRepository extends JpaRepository<GradingFee, Long> {

    List<GradingFee> findByActiveTrueOrderByGradingCompanyAscServiceLevelNameAsc();

    Optional<GradingFee> findFirstByGradingCompanyAndActiveTrueOrderByFeeSgdAscIdAsc(GradingCompany gradingCompany);
}
