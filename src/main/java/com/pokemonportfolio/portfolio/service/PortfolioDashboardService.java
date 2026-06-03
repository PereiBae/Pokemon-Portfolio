package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.auth.entity.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioDashboardService {

    private final PortfolioValuationService portfolioValuationService;

    public PortfolioDashboardService(PortfolioValuationService portfolioValuationService) {
        this.portfolioValuationService = portfolioValuationService;
    }

    @Transactional(readOnly = true)
    public PortfolioDashboardView dashboardFor(AppUser owner) {
        return portfolioValuationService.calculateCurrentValue(owner);
    }
}

