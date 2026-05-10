package com.tradingsim.strategy;

/**
 * Typed strategy configuration contract used by application services.
 */
public sealed interface StrategyConfig permits SmaCrossConfig, MeanReversionConfig {

    StrategyType strategyType();
}
