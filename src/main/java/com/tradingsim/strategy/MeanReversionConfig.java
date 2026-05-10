package com.tradingsim.strategy;

import java.math.BigDecimal;

/**
 * Strongly typed input parameters for mean reversion strategy runs.
 */
public record MeanReversionConfig(int window, BigDecimal thresholdPct) implements StrategyConfig {

    public MeanReversionConfig {
        if (window < 2) {
            throw new IllegalArgumentException("Expected meanReversionWindow >= 2.");
        }
        if (thresholdPct == null || thresholdPct.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expected meanReversionThresholdPct > 0.");
        }
    }

    @Override
    public StrategyType strategyType() {
        return StrategyType.MEAN_REVERSION;
    }
}
