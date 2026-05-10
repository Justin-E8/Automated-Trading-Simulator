package com.tradingsim.engine;

public record PerformanceMetrics(
        double totalReturnPct,
        double maxDrawdownPct,
        double sharpeRatio,
        double winRatePct,
        double profitFactor,
        double expectancy,
        double averageWin,
        double averageLoss,
        double exposureTimePct,
        long tradeCount
) {
}
