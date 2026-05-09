package com.tradingsim.engine;

import com.tradingsim.domain.Trade;

import java.math.BigDecimal;
import java.util.List;

public record SimulationResult(
        String strategyName,
        BigDecimal startingCash,
        BigDecimal endingEquity,
        List<Trade> trades,
        List<EquityPoint> equityCurve,
        PerformanceMetrics metrics
) {
}
