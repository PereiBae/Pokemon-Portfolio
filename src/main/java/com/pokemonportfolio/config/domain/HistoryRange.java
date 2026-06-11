package com.pokemonportfolio.config.domain;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum HistoryRange {
    LAST_7_DAYS("7D", "7D"),
    LAST_1_MONTH("1M", "1M"),
    LAST_3_MONTHS("3M", "3M"),
    LAST_1_YEAR("1Y", "1Y"),
    ALL("ALL", "All");

    private final String code;
    private final String label;

    HistoryRange(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public Optional<OffsetDateTime> since(OffsetDateTime now) {
        return switch (this) {
            case LAST_7_DAYS -> Optional.of(now.minusDays(7));
            case LAST_1_MONTH -> Optional.of(now.minusMonths(1));
            case LAST_3_MONTHS -> Optional.of(now.minusMonths(3));
            case LAST_1_YEAR -> Optional.of(now.minusYears(1));
            case ALL -> Optional.empty();
        };
    }

    public static HistoryRange fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ALL;
        }
        return Arrays.stream(values())
                .filter(range -> range.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(ALL);
    }

    public static List<HistoryRange> options() {
        return List.of(values());
    }
}
