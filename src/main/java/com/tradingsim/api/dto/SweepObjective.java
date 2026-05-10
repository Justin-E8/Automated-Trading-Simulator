package com.tradingsim.api.dto;

/**
 * Supported objective metrics for parameter sweep ranking.
 */
public enum SweepObjective {
    TOTAL_RETURN_PCT("total-return-pct"),
    MAX_DRAWDOWN_PCT("max-drawdown-pct"),
    SHARPE_RATIO("sharpe-ratio"),
    WIN_RATE_PCT("win-rate-pct"),
    PROFIT_FACTOR("profit-factor"),
    EXPECTANCY("expectancy");

    private final String apiValue;

    SweepObjective(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static SweepObjective fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "optimizeFor is required. Supported values: total-return-pct, max-drawdown-pct, sharpe-ratio, win-rate-pct, profit-factor, expectancy."
            );
        }
        for (SweepObjective objective : values()) {
            if (objective.apiValue.equalsIgnoreCase(value)) {
                return objective;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported optimizeFor '" + value + "'. Supported values: total-return-pct, max-drawdown-pct, sharpe-ratio, win-rate-pct, profit-factor, expectancy."
        );
    }
}
