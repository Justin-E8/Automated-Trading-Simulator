package com.tradingsim.engine;

/**
 * Aggregated metrics calculated after simulation completion.
 *
 * @param totalReturnPct   percentage return from initial cash to ending equity
 * @param maxDrawdownPct   maximum peak-to-trough decline in equity
 * @param sharpeRatio      simplified annualized Sharpe ratio approximation
 * @param winRatePct       percentage of profitable closed trades
 * @param profitFactor     gross profit / gross loss magnitude
 * @param expectancy       average realized PnL per closed trade
 * @param averageWin       average realized PnL across winning trades
 * @param averageLoss      average realized PnL across losing trades
 * @param exposureTimePct  percentage of candles with open position exposure
 * @param tradeCount       number of emitted trade events
 */
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
