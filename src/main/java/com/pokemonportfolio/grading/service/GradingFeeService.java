package com.pokemonportfolio.grading.service;

import com.pokemonportfolio.config.domain.GradingCompany;
import com.pokemonportfolio.grading.entity.GradingFee;
import com.pokemonportfolio.grading.repository.GradingFeeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingFeeService {

    private final GradingFeeRepository gradingFeeRepository;

    public GradingFeeService(GradingFeeRepository gradingFeeRepository) {
        this.gradingFeeRepository = gradingFeeRepository;
    }

    @Transactional(readOnly = true)
    public List<GradingFeeOptionView> listActiveFees() {
        return gradingFeeRepository.findByActiveTrueOrderByGradingCompanyAscServiceLevelNameAsc().stream()
                .map(GradingFeeOptionView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GradingFee requireActiveFee(Long id) {
        return gradingFeeRepository.findById(id)
                .filter(GradingFee::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Active grading fee not found"));
    }

    @Transactional(readOnly = true)
    public GradingFee defaultPsaFee() {
        return gradingFeeRepository.findFirstByGradingCompanyAndActiveTrueOrderByFeeSgdAscIdAsc(GradingCompany.PSA)
                .orElseThrow(() -> new IllegalStateException("No active PSA grading fee is configured"));
    }
}
