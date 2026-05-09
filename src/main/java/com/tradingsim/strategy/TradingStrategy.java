package com.tradingsim.strategy;

import com.tradingsim.domain.Candle;

import java.util.List;

public interface TradingStrategy {

    String name();

    StrategySignal generateSignal(List<Candle> candles, long openQuantity);
}
