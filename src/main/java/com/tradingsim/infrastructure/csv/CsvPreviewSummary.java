package com.tradingsim.infrastructure.csv;

import com.tradingsim.domain.Candle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Summary metadata returned when previewing uploaded CSV candles.
 */
public record CsvPreviewSummary(
        long candleCount,
        LocalDateTime startTimestamp,
        LocalDateTime endTimestamp,
        BigDecimal minClose,
        BigDecimal maxClose,
        List<Candle> sampleCandles
) {
}
