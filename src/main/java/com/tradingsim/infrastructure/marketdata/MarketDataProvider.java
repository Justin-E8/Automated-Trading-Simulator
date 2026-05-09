package com.tradingsim.infrastructure.marketdata;

import com.tradingsim.domain.Candle;

import java.time.LocalDate;
import java.util.List;

public interface MarketDataProvider {

    String providerName();

    List<Candle> fetchCandles(String symbol, LocalDate startDate, LocalDate endDate, String interval);
}
