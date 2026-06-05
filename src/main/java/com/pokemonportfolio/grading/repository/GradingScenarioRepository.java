package com.pokemonportfolio.grading.repository;

import com.pokemonportfolio.grading.entity.GradingAnalysis;
import com.pokemonportfolio.grading.entity.GradingScenario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradingScenarioRepository extends JpaRepository<GradingScenario, Long> {

    List<GradingScenario> findByGradingAnalysisOrderByIdAsc(GradingAnalysis gradingAnalysis);
}
