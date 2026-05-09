package com.tradingsim.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MarketDataFetchResponse(
        String symbol,
        String interval,
        String provider,
        boolean cached,
        long candleCount,
        LocalDateTime startTimestamp,
        LocalDateTime endTimestamp,
        List<CandleDto> sampleCandles
) {
}
