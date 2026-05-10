package com.tradingsim.engine;

import com.tradingsim.domain.Candle;
import com.tradingsim.domain.OrderSide;
import com.tradingsim.strategy.MovingAverageCrossStrategy;
import com.tradingsim.strategy.StrategySignal;
import com.tradingsim.strategy.TradingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulationEngineTest {

    private final SimulationEngine simulationEngine = new SimulationEngine();

    @Test
    void run_executesTradesAndReturnsMetrics() {
        SimulationRequest request = new SimulationRequest(
                "AAPL",
                new BigDecimal("10000.00"),
                10,
                new BigDecimal("5.0"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                candles()
        );

        SimulationResult result = simulationEngine.run(request, new MovingAverageCrossStrategy(3, 5));

        assertThat(result.equityCurve()).hasSize(candles().size());
        assertThat(result.metrics().tradeCount()).isGreaterThanOrEqualTo(0);
        assertThat(result.endingEquity()).isNotNull();
        assertThat(result.metrics().profitFactor()).isGreaterThanOrEqualTo(0.0);
        assertThat(result.metrics().exposureTimePct()).isBetween(0.0, 100.0);
    }

    @Test
    void run_withoutCandles_throwsException() {
        SimulationRequest request = new SimulationRequest(
                "AAPL",
                new BigDecimal("10000.00"),
                10,
                new BigDecimal("5.0"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                List.of()
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> simulationEngine.run(request, new MovingAverageCrossStrategy(3, 5))
        );
    }

    @Test
    void run_appliesMaxPositionSizeAndSlippage() {
        SimulationRequest request = new SimulationRequest(
                "AAPL",
                new BigDecimal("10000.00"),
                10,
                BigDecimal.ZERO,
                new BigDecimal("100.0"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                4,
                0,
                List.of(
                        candle(0, "100.00"),
                        candle(1, "101.00")
                )
        );

        SimulationResult result = simulationEngine.run(
                request,
                scriptedStrategy(StrategySignal.BUY, StrategySignal.SELL)
        );

        assertThat(result.trades()).hasSize(2);
        assertThat(result.trades().get(0).quantity()).isEqualTo(4);
        assertThat(result.trades().get(0).price()).isEqualByComparingTo("101.0000");
        assertThat(result.trades().get(1).price()).isEqualByComparingTo("99.9900");
    }

    @Test
    void run_stopLossTriggersExitWithoutSellSignal() {
        SimulationRequest request = new SimulationRequest(
                "AAPL",
                new BigDecimal("10000.00"),
                5,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("3.0"),
                BigDecimal.ZERO,
                0,
                0,
                List.of(
                        candle(0, "100.00"),
                        candle(1, "95.00"),
                        candle(2, "94.00")
                )
        );

        SimulationResult result = simulationEngine.run(
                request,
                scriptedStrategy(StrategySignal.BUY, StrategySignal.HOLD, StrategySignal.HOLD)
        );

        assertThat(result.trades()).hasSize(2);
        assertThat(result.trades().get(0).side()).isEqualTo(OrderSide.BUY);
        assertThat(result.trades().get(1).side()).isEqualTo(OrderSide.SELL);
        assertThat(result.trades().get(1).timestamp()).isEqualTo(candle(1, "95.00").timestamp());
    }

    @Test
    void run_maxHoldingCandlesTriggersExit() {
        SimulationRequest request = new SimulationRequest(
                "AAPL",
                new BigDecimal("10000.00"),
                5,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                1,
                List.of(
                        candle(0, "100.00"),
                        candle(1, "101.00"),
                        candle(2, "102.00")
                )
        );

        SimulationResult result = simulationEngine.run(
                request,
                scriptedStrategy(StrategySignal.BUY, StrategySignal.HOLD, StrategySignal.HOLD)
        );

        assertThat(result.trades()).hasSize(2);
        assertThat(result.trades().get(1).side()).isEqualTo(OrderSide.SELL);
        assertThat(result.trades().get(1).timestamp()).isEqualTo(candle(1, "101.00").timestamp());
    }

    @Test
    void run_takeProfitTriggersExitWithoutSellSignal() {
        SimulationRequest request = new SimulationRequest(
                "AAPL",
                new BigDecimal("10000.00"),
                5,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("2.0"),
                0,
                0,
                List.of(
                        candle(0, "100.00"),
                        candle(1, "102.00"),
                        candle(2, "103.00")
                )
        );

        SimulationResult result = simulationEngine.run(
                request,
                scriptedStrategy(StrategySignal.BUY, StrategySignal.HOLD, StrategySignal.HOLD)
        );

        assertThat(result.trades()).hasSize(2);
        assertThat(result.trades().get(1).side()).isEqualTo(OrderSide.SELL);
        assertThat(result.trades().get(1).timestamp()).isEqualTo(candle(1, "102.00").timestamp());
    }

    @Test
    void run_whenMaxPositionSizeIsZero_usesQuantityPerTrade() {
        SimulationRequest request = new SimulationRequest(
                "AAPL",
                new BigDecimal("10000.00"),
                7,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                List.of(
                        candle(0, "100.00"),
                        candle(1, "101.00")
                )
        );

        SimulationResult result = simulationEngine.run(
                request,
                scriptedStrategy(StrategySignal.BUY, StrategySignal.SELL)
        );

        assertThat(result.trades()).hasSize(2);
        assertThat(result.trades().get(0).quantity()).isEqualTo(7);
    }

    @Test
    void run_negativeSlippage_throwsException() {
        SimulationRequest request = new SimulationRequest(
                "AAPL",
                new BigDecimal("10000.00"),
                5,
                BigDecimal.ZERO,
                new BigDecimal("-0.1"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                List.of(candle(0, "100.00"), candle(1, "101.00"))
        );

        assertThatThrownBy(() -> simulationEngine.run(request, scriptedStrategy(StrategySignal.BUY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slippageBps");
    }

    private TradingStrategy scriptedStrategy(StrategySignal... signals) {
        return new TradingStrategy() {
            @Override
            public String name() {
                return "Scripted";
            }

            @Override
            public StrategySignal generateSignal(List<Candle> candles, long openQuantity) {
                int index = candles.size() - 1;
                if (index >= 0 && index < signals.length) {
                    return signals[index];
                }
                return StrategySignal.HOLD;
            }
        };
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
