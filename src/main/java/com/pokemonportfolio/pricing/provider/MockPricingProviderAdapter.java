package com.pokemonportfolio.pricing.provider;

import com.pokemonportfolio.catalog.entity.Card;
import com.pokemonportfolio.config.domain.ConfidenceRating;
import com.pokemonportfolio.pricing.service.MoneyCalculationSupport;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MockPricingProviderAdapter implements PricingProviderAdapter {

    private final PricingProviderProperties properties;

    public MockPricingProviderAdapter(PricingProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerName() {
        return "MockPricingProvider";
    }

    @Override
    public boolean isEnabled() {
        return properties.isMockEnabled();
    }

    @Override
    public PricingProviderPrice fetchCardPrice(Card card) {
        int bucket = Math.abs((card.getName() + card.getCardNumber()).hashCode() % 9000);
        BigDecimal mockPrice = MoneyCalculationSupport.money(BigDecimal.valueOf(20_00L + bucket, 2));
        return new PricingProviderPrice(
                providerName(),
                mockPrice,
                "SGD",
                BigDecimal.ONE.setScale(8),
                mockPrice,
                ConfidenceRating.MEDIUM,
                "SGD-only mock price generated for local Vertical Slice 1 valuation.");
    }
}

