package com.tradingsim.strategy;

import com.tradingsim.domain.Candle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeanReversionStrategyTest {

    @Test
    void generateSignal_buysWhenPriceFallsBelowLowerBand() {
        MeanReversionStrategy strategy = new MeanReversionStrategy(3, new BigDecimal("2.0"));
        List<Candle> candles = List.of(
                candle(0, "100.0"),
                candle(1, "100.0"),
                candle(2, "95.0")
        );

        assertThat(strategy.generateSignal(candles, 0)).isEqualTo(StrategySignal.BUY);
    }

    @Test
    void generateSignal_sellsWhenPriceMovesAboveUpperBand() {
        MeanReversionStrategy strategy = new MeanReversionStrategy(3, new BigDecimal("2.0"));
        List<Candle> candles = List.of(
                candle(0, "100.0"),
                candle(1, "100.0"),
                candle(2, "105.0")
        );

        assertThat(strategy.generateSignal(candles, 10)).isEqualTo(StrategySignal.SELL);
    }

    @Test
    void generateSignal_holdsWhenNotEnoughData() {
        MeanReversionStrategy strategy = new MeanReversionStrategy(5, new BigDecimal("2.0"));
        List<Candle> candles = List.of(
                candle(0, "100.0"),
                candle(1, "99.0"),
                candle(2, "98.0")
        );

        assertThat(strategy.generateSignal(candles, 0)).isEqualTo(StrategySignal.HOLD);
    }

    @Test
    void generateSignal_holdsWhenSignalConflictsWithCurrentPosition() {
        MeanReversionStrategy strategy = new MeanReversionStrategy(3, new BigDecimal("2.0"));
        List<Candle> candles = List.of(
                candle(0, "100.0"),
                candle(1, "100.0"),
                candle(2, "95.0")
        );

        assertThat(strategy.generateSignal(candles, 5)).isEqualTo(StrategySignal.HOLD);
    }

    private Candle candle(int dayOffset, String close) {
        LocalDateTime timestamp = LocalDateTime.parse("2025-01-01T09:30:00").plusDays(dayOffset);
        BigDecimal price = new BigDecimal(close);
        return new Candle(timestamp, price, price, price, price, 1000);
    }
}
