package com.pokemonportfolio.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyCalculationSupport {

    private static final Locale SINGAPORE = Locale.forLanguageTag("en-SG");

    private MoneyCalculationSupport() {
    }

    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 4, RoundingMode.HALF_UP);
    }

    public static String formatSgd(BigDecimal value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(SINGAPORE);
        format.setCurrency(java.util.Currency.getInstance("SGD"));
        return format.format(money(value));
    }
}

