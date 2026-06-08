package com.pokemonportfolio.pricing.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pokemonportfolio.pricing.service.ExchangeRateRefreshService;
import org.junit.jupiter.api.Test;

class ExchangeRateRefreshJobTest {

    @Test
    void runDailyRefreshDelegatesToRefreshService() {
        ExchangeRateRefreshService service = mock(ExchangeRateRefreshService.class);
        ExchangeRateRefreshJob job = new ExchangeRateRefreshJob(service);

        job.runDailyRefresh();

        verify(service).refreshDefaultSgdRates();
    }
}
