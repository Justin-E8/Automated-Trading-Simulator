package com.tradingsim.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        long tradeCount
) {
}
