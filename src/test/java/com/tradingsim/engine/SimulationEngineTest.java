package com.tradingsim.engine;

import com.tradingsim.domain.Candle;
import com.tradingsim.strategy.MovingAverageCrossStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationEngineTest {

    private final SimulationEngine simulationEngine = new SimulationEngine();

    @Test
    void run_executesTradesAndReturnsMetrics() {
        SimulationRequest request = new SimulationRequest(
                "AAPL",
                new BigDecimal("10000.00"),
                10,
                new BigDecimal("5.0"),
                candles()
        );

        SimulationResult result = simulationEngine.run(request, new MovingAverageCrossStrategy(3, 5));

        assertThat(result.equityCurve()).hasSize(candles().size());
        assertThat(result.metrics().tradeCount()).isGreaterThanOrEqualTo(0);
        assertThat(result.endingEquity()).isNotNull();
    }

    @Test
    void run_withoutCandles_throwsException() {
        SimulationRequest request = new SimulationRequest(
                "AAPL",
                new BigDecimal("10000.00"),
                10,
                new BigDecimal("5.0"),
                List.of()
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> simulationEngine.run(request, new MovingAverageCrossStrategy(3, 5))
        );
    }

    private List<Candle> candles() {
        return List.of(
                candle(0, "100.00"),
                candle(1, "101.50"),
                candle(2, "102.20"),
                candle(3, "103.40"),
                candle(4, "104.80"),
                candle(5, "103.70"),
                candle(6, "102.30"),
                candle(7, "101.10"),
                candle(8, "102.50"),
                candle(9, "104.00"),
                candle(10, "105.40")
        );
    }

    private Candle candle(int dayOffset, String close) {
        LocalDateTime timestamp = LocalDateTime.parse("2025-01-01T09:30:00").plusDays(dayOffset);
        BigDecimal price = new BigDecimal(close);
        return new Candle(timestamp, price, price, price, price, 1000);
    }
}
