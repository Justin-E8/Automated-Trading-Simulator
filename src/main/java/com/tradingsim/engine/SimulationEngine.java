package com.tradingsim.engine;

import com.tradingsim.domain.Candle;
import com.tradingsim.domain.OrderSide;
import com.tradingsim.domain.Trade;
import com.tradingsim.strategy.StrategySignal;
import com.tradingsim.strategy.TradingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic backtest engine that applies strategy signals to candles in order.
 *
 * <p>Tracks trades, equity curve, and summary metrics for a single simulation run.</p>
 */
@Component
public class SimulationEngine {

    /**
     * Executes one full simulation run for the provided strategy and request parameters.
     */
    public SimulationResult run(SimulationRequest request, TradingStrategy strategy) {
        if (request.candles() == null || request.candles().isEmpty()) {
            throw new IllegalArgumentException("Candles are required for a simulation run.");
        }
        if (request.quantityPerTrade() <= 0) {
            throw new IllegalArgumentException("Quantity per trade must be positive.");
        }

        BigDecimal cash = request.initialCash();
        long openQuantity = 0L;
        BigDecimal entryPrice = BigDecimal.ZERO;
        BigDecimal entryFee = BigDecimal.ZERO;

        List<Candle> history = new ArrayList<>();
        List<Trade> trades = new ArrayList<>();
        List<EquityPoint> equityCurve = new ArrayList<>();

        int winningClosedTrades = 0;
        int closedTrades = 0;

        for (Candle candle : request.candles()) {
            history.add(candle);
            BigDecimal price = candle.close();
            StrategySignal signal = strategy.generateSignal(history, openQuantity);

            if (signal == StrategySignal.BUY && openQuantity == 0L) {
                BigDecimal notional = price.multiply(BigDecimal.valueOf(request.quantityPerTrade()));
                BigDecimal fee = calculateFee(notional, request.feeBps());
                BigDecimal totalCost = notional.add(fee);

                if (cash.compareTo(totalCost) >= 0) {
                    cash = cash.subtract(totalCost);
                    openQuantity = request.quantityPerTrade();
                    entryPrice = price;
                    entryFee = fee;

                    trades.add(new Trade(
                            candle.timestamp(),
                            request.symbol(),
                            OrderSide.BUY,
                            openQuantity,
                            price,
                            fee,
                            BigDecimal.ZERO
                    ));
                }
            } else if (signal == StrategySignal.SELL && openQuantity > 0L) {
                BigDecimal notional = price.multiply(BigDecimal.valueOf(openQuantity));
                BigDecimal fee = calculateFee(notional, request.feeBps());
                cash = cash.add(notional.subtract(fee));

                BigDecimal grossPnl = price.subtract(entryPrice).multiply(BigDecimal.valueOf(openQuantity));
                BigDecimal realizedPnl = grossPnl.subtract(entryFee).subtract(fee).setScale(4, RoundingMode.HALF_UP);

                trades.add(new Trade(
                        candle.timestamp(),
                        request.symbol(),
                        OrderSide.SELL,
                        openQuantity,
                        price,
                        fee,
                        realizedPnl
                ));

                closedTrades++;
                if (realizedPnl.compareTo(BigDecimal.ZERO) > 0) {
                    winningClosedTrades++;
                }

                openQuantity = 0L;
                entryPrice = BigDecimal.ZERO;
                entryFee = BigDecimal.ZERO;
            }

            BigDecimal equity = cash.add(price.multiply(BigDecimal.valueOf(openQuantity)));
            equityCurve.add(new EquityPoint(candle.timestamp(), equity.setScale(4, RoundingMode.HALF_UP)));
        }

        BigDecimal endingEquity = equityCurve.get(equityCurve.size() - 1).equity();
        PerformanceMetrics metrics = calculateMetrics(
                request.initialCash(),
                endingEquity,
                equityCurve,
                trades.size(),
                closedTrades,
                winningClosedTrades
        );

        return new SimulationResult(
                strategy.name(),
                request.initialCash(),
                endingEquity,
                trades,
                equityCurve,
                metrics
        );
    }

    private BigDecimal calculateFee(BigDecimal notional, BigDecimal feeBps) {
        return notional.multiply(feeBps)
                .divide(BigDecimal.valueOf(10_000), 8, RoundingMode.HALF_UP)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private PerformanceMetrics calculateMetrics(
            BigDecimal initialCash,
            BigDecimal endingEquity,
            List<EquityPoint> equityCurve,
            int totalTrades,
            int closedTrades,
            int winningClosedTrades
    ) {
        double totalReturnPct = endingEquity.subtract(initialCash)
                .multiply(BigDecimal.valueOf(100))
                .divide(initialCash, 6, RoundingMode.HALF_UP)
                .doubleValue();

        double maxDrawdownPct = calculateMaxDrawdown(equityCurve);
        double sharpeRatio = calculateSharpeRatio(equityCurve);
        double winRatePct = closedTrades == 0 ? 0.0 : (winningClosedTrades * 100.0) / closedTrades;

        return new PerformanceMetrics(
                roundMetric(totalReturnPct),
                roundMetric(maxDrawdownPct),
                roundMetric(sharpeRatio),
                roundMetric(winRatePct),
                totalTrades
        );
    }

    private double calculateMaxDrawdown(List<EquityPoint> equityCurve) {
        BigDecimal peak = equityCurve.get(0).equity();
        double maxDrawdown = 0.0;

        for (EquityPoint point : equityCurve) {
            if (point.equity().compareTo(peak) > 0) {
                peak = point.equity();
            }
            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                double drawdown = peak.subtract(point.equity())
                        .divide(peak, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                if (drawdown > maxDrawdown) {
                    maxDrawdown = drawdown;
                }
            }
        }
        return maxDrawdown;
    }

    private double calculateSharpeRatio(List<EquityPoint> equityCurve) {
        if (equityCurve.size() < 2) {
            return 0.0;
        }

        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < equityCurve.size(); i++) {
            BigDecimal previous = equityCurve.get(i - 1).equity();
            BigDecimal current = equityCurve.get(i).equity();
            if (previous.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            double r = current.subtract(previous)
                    .divide(previous, 8, RoundingMode.HALF_UP)
                    .doubleValue();
            returns.add(r);
        }

        if (returns.isEmpty()) {
            return 0.0;
        }

        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);
        if (stdDev == 0.0) {
            return 0.0;
        }

        return (mean / stdDev) * Math.sqrt(252.0);
    }

    private double roundMetric(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
