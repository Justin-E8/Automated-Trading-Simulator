package com.tradingsim.strategy;

import com.tradingsim.domain.Candle;

import java.util.List;

/**
 * Contract implemented by all strategy modules used by the simulation engine.
 */
public interface TradingStrategy {

    /**
     * Human-readable strategy label shown in result payloads.
     */
    String name();

    /**
     * Generates a trading action for the latest candle in the provided history.
     */
    StrategySignal generateSignal(List<Candle> candles, long openQuantity);
}
