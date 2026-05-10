package com.tradingsim.engine;

import com.tradingsim.domain.Trade;

import java.math.BigDecimal;
import java.util.List;

/**
 * Complete result object returned by the simulation engine.
 *
 * @param strategyName strategy label used for this run
 * @param startingCash initial cash provided to simulation
 * @param endingEquity final account equity after processing all candles
 * @param trades       emitted trade events in chronological order
 * @param equityCurve  equity point series across all candles
 * @param metrics      summary performance metrics
 */
public record SimulationResult(
        String strategyName,
        BigDecimal startingCash,
        BigDecimal endingEquity,
        List<Trade> trades,
        List<EquityPoint> equityCurve,
        PerformanceMetrics metrics
) {
}
