package com.tradingsim.engine;

public record PerformanceMetrics(
        double totalReturnPct,
        double maxDrawdownPct,
        double sharpeRatio,
        double winRatePct,
        long tradeCount
) {
}
