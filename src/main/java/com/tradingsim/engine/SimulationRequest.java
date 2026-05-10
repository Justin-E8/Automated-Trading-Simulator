package com.tradingsim.engine;

import com.tradingsim.domain.Candle;

import java.math.BigDecimal;
import java.util.List;

public record SimulationRequest(
        String symbol,
        BigDecimal initialCash,
        long quantityPerTrade,
        BigDecimal feeBps,
        BigDecimal slippageBps,
        BigDecimal stopLossPct,
        BigDecimal takeProfitPct,
        long maxPositionSize,
        int maxHoldingCandles,
        List<Candle> candles
) {
}
