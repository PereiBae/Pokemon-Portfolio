package com.pokemonportfolio.portfolio.scheduler;

import com.pokemonportfolio.auth.repository.AppUserRepository;
import com.pokemonportfolio.portfolio.service.PortfolioValuationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PortfolioValuationJob {

    private final AppUserRepository appUserRepository;
    private final PortfolioValuationService portfolioValuationService;

    public PortfolioValuationJob(
            AppUserRepository appUserRepository,
            PortfolioValuationService portfolioValuationService) {
        this.appUserRepository = appUserRepository;
        this.portfolioValuationService = portfolioValuationService;
    }

    @Scheduled(cron = "${app.jobs.portfolio-valuation-cron}")
    public void runDailySnapshot() {
        appUserRepository.findAll().forEach(portfolioValuationService::createSnapshot);
    }
}

