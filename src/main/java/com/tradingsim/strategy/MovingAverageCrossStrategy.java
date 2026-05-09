package com.tradingsim.strategy;

import com.tradingsim.domain.Candle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class MovingAverageCrossStrategy implements TradingStrategy {

    private final int shortWindow;
    private final int longWindow;

    public MovingAverageCrossStrategy(int shortWindow, int longWindow) {
        if (shortWindow < 2 || longWindow < 3 || shortWindow >= longWindow) {
            throw new IllegalArgumentException("Expected 2 <= shortWindow < longWindow");
        }
        this.shortWindow = shortWindow;
        this.longWindow = longWindow;
    }

    @Override
    public String name() {
        return "SMA(" + shortWindow + "," + longWindow + ")";
    }

    @Override
    public StrategySignal generateSignal(List<Candle> candles, long openQuantity) {
        if (candles.size() < longWindow) {
            return StrategySignal.HOLD;
        }

        BigDecimal shortSma = simpleMovingAverage(candles, shortWindow);
        BigDecimal longSma = simpleMovingAverage(candles, longWindow);

        int cmp = shortSma.compareTo(longSma);
        if (cmp > 0 && openQuantity == 0) {
            return StrategySignal.BUY;
        }
        if (cmp < 0 && openQuantity > 0) {
            return StrategySignal.SELL;
        }
        return StrategySignal.HOLD;
    }

    private BigDecimal simpleMovingAverage(List<Candle> candles, int window) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = candles.size() - window; i < candles.size(); i++) {
            sum = sum.add(candles.get(i).close());
        }
        return sum.divide(BigDecimal.valueOf(window), 8, RoundingMode.HALF_UP);
    }
}
