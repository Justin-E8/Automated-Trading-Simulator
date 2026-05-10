package com.tradingsim.engine;

import com.tradingsim.domain.Candle;
import com.tradingsim.strategy.MovingAverageCrossStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class SimulationEnginePerformanceTest {

    private final SimulationEngine simulationEngine = new SimulationEngine();

    @Test
    void run_largeDataset_completesWithinReasonableTime() {
        List<Candle> candles = generateCandles(8_000);
        SimulationRequest request = new SimulationRequest(
                "PERF",
                new BigDecimal("50000.00"),
                10,
                new BigDecimal("2.0"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                candles
        );

        SimulationResult result = assertTimeout(
                Duration.ofSeconds(3),
                () -> simulationEngine.run(request, new MovingAverageCrossStrategy(5, 20))
        );

        assertThat(result.equityCurve()).hasSize(8_000);
    }

    private List<Candle> generateCandles(int count) {
        List<Candle> candles = new ArrayList<>(count);
        LocalDateTime start = LocalDateTime.parse("2025-01-01T09:30:00");
        for (int i = 0; i < count; i++) {
            double trend = i * 0.02;
            double oscillation = Math.sin(i / 8.0) * 1.5;
            BigDecimal close = BigDecimal.valueOf(100.0 + trend + oscillation).setScale(4, java.math.RoundingMode.HALF_UP);
            candles.add(new Candle(
                    start.plusMinutes(i),
                    close.subtract(new BigDecimal("0.20")),
                    close.add(new BigDecimal("0.35")),
                    close.subtract(new BigDecimal("0.45")),
                    close,
                    1_000 + i
            ));
        }
        return candles;
    }
}
