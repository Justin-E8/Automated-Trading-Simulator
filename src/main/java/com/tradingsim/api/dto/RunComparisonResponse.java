package com.tradingsim.api.dto;

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
            long tradeCountDelta
    ) {
    }
}
