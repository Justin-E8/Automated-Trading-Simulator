package com.tradingsim.api.dto;

import com.tradingsim.domain.OrderSide;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BacktestRunResponse(
        Long runId,
        LocalDateTime createdAt,
        String symbol,
        String strategyName,
        BigDecimal startingCash,
        BigDecimal endingEquity,
        MetricsDto metrics,
        List<TradeDto> trades,
        List<EquityPointDto> equityCurve
) {
    public record MetricsDto(
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

    public record TradeDto(
            LocalDateTime timestamp,
            String symbol,
            OrderSide side,
            long quantity,
            BigDecimal price,
            BigDecimal fee,
            BigDecimal realizedPnl
    ) {
    }

    public record EquityPointDto(
            LocalDateTime timestamp,
            BigDecimal equity
    ) {
    }
}
