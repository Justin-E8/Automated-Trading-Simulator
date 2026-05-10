package com.tradingsim.strategy;

/**
 * Strongly typed input parameters for SMA cross strategy runs.
 */
public record SmaCrossConfig(int shortWindow, int longWindow) implements StrategyConfig {

    public SmaCrossConfig {
        if (shortWindow < 2) {
            throw new IllegalArgumentException("Expected shortWindow >= 2.");
        }
        if (longWindow < 3) {
            throw new IllegalArgumentException("Expected longWindow >= 3.");
        }
        if (shortWindow >= longWindow) {
            throw new IllegalArgumentException("Expected shortWindow < longWindow.");
        }
    }

    @Override
    public StrategyType strategyType() {
        return StrategyType.SMA_CROSS;
    }
}
