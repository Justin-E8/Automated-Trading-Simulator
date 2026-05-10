package com.tradingsim.strategy;

import com.tradingsim.domain.Candle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Buys when price is sufficiently below moving average and exits above it.
 */
public class MeanReversionStrategy implements TradingStrategy {

    private final int lookbackWindow;
    private final BigDecimal thresholdPct;

    public MeanReversionStrategy(int lookbackWindow, BigDecimal thresholdPct) {
        if (lookbackWindow < 2) {
            throw new IllegalArgumentException("Expected lookbackWindow >= 2.");
        }
        if (thresholdPct == null || thresholdPct.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expected thresholdPct > 0.");
        }
        this.lookbackWindow = lookbackWindow;
        this.thresholdPct = thresholdPct;
    }

    @Override
    public String name() {
        return "MeanReversion(" + lookbackWindow + "," + thresholdPct.stripTrailingZeros().toPlainString() + "%)";
    }

    @Override
    public StrategySignal generateSignal(List<Candle> candles, long openQuantity) {
        if (candles.size() < lookbackWindow) {
            return StrategySignal.HOLD;
        }

        BigDecimal movingAverage = simpleMovingAverage(candles, lookbackWindow);
        BigDecimal multiplier = thresholdPct.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        BigDecimal lowerBand = movingAverage.multiply(BigDecimal.ONE.subtract(multiplier));
        BigDecimal upperBand = movingAverage.multiply(BigDecimal.ONE.add(multiplier));
        BigDecimal latestClose = candles.get(candles.size() - 1).close();

        if (latestClose.compareTo(lowerBand) <= 0 && openQuantity == 0L) {
            return StrategySignal.BUY;
        }
        if (latestClose.compareTo(upperBand) >= 0 && openQuantity > 0L) {
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
