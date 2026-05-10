package com.tradingsim.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight run summary for paged history responses.
 */
public record RunSummaryResponse(
        Long runId,
        LocalDateTime createdAt,
        String symbol,
        String strategyName,
        BigDecimal startingCash,
        BigDecimal endingEquity,
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
