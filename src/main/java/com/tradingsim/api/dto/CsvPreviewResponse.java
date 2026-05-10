package com.tradingsim.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Summary response returned after parsing an uploaded CSV.
 */
public record CsvPreviewResponse(
        long candleCount,
        LocalDateTime startTimestamp,
        LocalDateTime endTimestamp,
        BigDecimal minClose,
        BigDecimal maxClose,
        List<CandleDto> sampleCandles
) {
}
