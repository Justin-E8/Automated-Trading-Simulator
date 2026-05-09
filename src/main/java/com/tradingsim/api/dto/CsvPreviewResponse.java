package com.tradingsim.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CsvPreviewResponse(
        long candleCount,
        LocalDateTime startTimestamp,
        LocalDateTime endTimestamp,
        BigDecimal minClose,
        BigDecimal maxClose,
        List<CandleDto> sampleCandles
) {
}
