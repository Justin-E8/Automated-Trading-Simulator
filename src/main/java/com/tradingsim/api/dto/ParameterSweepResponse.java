package com.tradingsim.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ranked output from parameter sweep/grid-search execution.
 */
public record ParameterSweepResponse(
        String strategy,
        String optimizeFor,
        int evaluatedCombinations,
        int returnedCombinations,
        SweepResult bestResult,
        List<SweepResult> results
) {
    public record SweepResult(
            int rank,
            String strategyName,
            SweepParameters parameters,
            BigDecimal endingEquity,
            BacktestRunResponse.MetricsDto metrics,
            double objectiveScore
    ) {
    }

    public record SweepParameters(
            Integer shortWindow,
            Integer longWindow,
            Integer meanReversionWindow,
            BigDecimal meanReversionThresholdPct
    ) {
    }
}
