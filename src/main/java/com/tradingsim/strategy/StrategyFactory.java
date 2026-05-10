package com.tradingsim.strategy;

import org.springframework.stereotype.Component;

/**
 * Creates executable strategy instances from validated typed configs.
 */
@Component
public class StrategyFactory {

    public TradingStrategy create(StrategyConfig config) {
        return switch (config) {
            case SmaCrossConfig sma -> new MovingAverageCrossStrategy(sma.shortWindow(), sma.longWindow());
            case MeanReversionConfig meanReversion ->
                    new MeanReversionStrategy(meanReversion.window(), meanReversion.thresholdPct());
        };
    }
}
