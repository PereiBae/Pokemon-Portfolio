package com.pokemonportfolio.portfolio.service;

import com.pokemonportfolio.alerts.service.AlertViewService;
import com.pokemonportfolio.auth.entity.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioDashboardService {

    private final PortfolioValuationService portfolioValuationService;
    private final AlertViewService alertViewService;

    public PortfolioDashboardService(
            PortfolioValuationService portfolioValuationService,
            AlertViewService alertViewService) {
        this.portfolioValuationService = portfolioValuationService;
        this.alertViewService = alertViewService;
    }

    @Transactional(readOnly = true)
    public PortfolioDashboardView dashboardFor(AppUser owner) {
        PortfolioDashboardView valuation = portfolioValuationService.calculateCurrentValue(owner);
        return valuation.withAlerts(
                (int) alertViewService.activeCount(owner),
                alertViewService.latestActiveAlerts(owner, 3));
    }
}
