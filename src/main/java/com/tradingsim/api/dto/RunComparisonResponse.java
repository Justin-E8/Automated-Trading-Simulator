package com.tradingsim.api.dto;

/**
 * Pairwise run comparison payload with right-minus-left metric deltas.
 */
public record RunComparisonResponse(
        RunSummaryResponse leftRun,
        RunSummaryResponse rightRun,
        DeltaMetrics delta
) {
    public record DeltaMetrics(
            double endingEquityDelta,
            double totalReturnPctDelta,
            double maxDrawdownPctDelta,
            double sharpeRatioDelta,
            double winRatePctDelta,
            double profitFactorDelta,
            double expectancyDelta,
            double averageWinDelta,
            double averageLossDelta,
            double exposureTimePctDelta,
            long tradeCountDelta
    ) {
    }
}
