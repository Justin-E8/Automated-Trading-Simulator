package com.tradingsim.engine;

import com.tradingsim.domain.Candle;

import java.math.BigDecimal;
import java.util.List;

public record SimulationRequest(
        String symbol,
        BigDecimal initialCash,
        long quantityPerTrade,
        BigDecimal feeBps,
        List<Candle> candles
) {
}
