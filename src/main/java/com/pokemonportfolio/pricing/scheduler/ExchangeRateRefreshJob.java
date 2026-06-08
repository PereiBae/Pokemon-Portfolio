package com.pokemonportfolio.pricing.scheduler;

import com.pokemonportfolio.pricing.service.ExchangeRateRefreshService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateRefreshJob {

    private final ExchangeRateRefreshService exchangeRateRefreshService;

    public ExchangeRateRefreshJob(ExchangeRateRefreshService exchangeRateRefreshService) {
        this.exchangeRateRefreshService = exchangeRateRefreshService;
    }

    @Scheduled(cron = "${app.jobs.exchange-rate-refresh-cron}")
    public void runDailyRefresh() {
        exchangeRateRefreshService.refreshDefaultSgdRates();
    }
}
