package com.pokemonportfolio.pricing.scheduler;

import com.pokemonportfolio.pricing.service.MarketValuationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PriceRefreshJob {

    private final MarketValuationService marketValuationService;

    public PriceRefreshJob(MarketValuationService marketValuationService) {
        this.marketValuationService = marketValuationService;
    }

    @Scheduled(cron = "${app.jobs.price-refresh-cron}")
    public void runDailyRefresh() {
        marketValuationService.refreshAllActiveCards();
    }
}

