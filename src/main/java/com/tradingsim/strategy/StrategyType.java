package com.tradingsim.strategy;

/**
 * Supported strategy identifiers exposed through API/UI requests.
 */
public enum StrategyType {
    SMA_CROSS("sma-cross"),
    MEAN_REVERSION("mean-reversion");

    private final String apiValue;

    StrategyType(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static StrategyType fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Strategy is required. Supported values: sma-cross, mean-reversion.");
        }
        for (StrategyType type : values()) {
            if (type.apiValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported strategy '" + value + "'. Supported values: sma-cross, mean-reversion."
        );
    }
}
