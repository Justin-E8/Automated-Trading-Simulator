package com.tradingsim.infrastructure.marketdata;

import com.tradingsim.domain.Candle;

import java.util.List;

public record MarketDataFetchResult(
        String symbol,
        String interval,
        String provider,
        boolean cached,
        List<Candle> candles
) {
}
