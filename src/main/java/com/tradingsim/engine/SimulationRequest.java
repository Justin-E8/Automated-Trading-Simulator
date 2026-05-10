package com.tradingsim.engine;

import com.tradingsim.domain.Candle;

import java.math.BigDecimal;
import java.util.List;

/**
 * Input payload for a single simulation run.
 *
 * @param symbol            traded symbol label
 * @param initialCash       starting account cash
 * @param quantityPerTrade  requested shares per entry
 * @param feeBps            transaction fee in basis points
 * @param slippageBps       execution slippage in basis points
 * @param stopLossPct       stop-loss threshold percent (0 disables)
 * @param takeProfitPct     take-profit threshold percent (0 disables)
 * @param maxPositionSize   max shares allowed in position (0 means quantityPerTrade)
 * @param maxHoldingCandles max candles to hold an open position (0 disables)
 * @param candles           ordered candle series to backtest
 */
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
