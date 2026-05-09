package com.tradingsim.infrastructure.marketdata;

import com.tradingsim.application.CandleValidationService;
import com.tradingsim.domain.Candle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataService {

    private static final String FALLBACK_PROVIDER = "alphavantage";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final CandleValidationService candleValidationService;
    private final String defaultProvider;
    private final Map<String, MarketDataProvider> providersByName;
    private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public MarketDataService(
            List<MarketDataProvider> providers,
            CandleValidationService candleValidationService,
            @Value("${market-data.default-provider:" + FALLBACK_PROVIDER + "}") String defaultProvider
    ) {
        this.candleValidationService = candleValidationService;
        this.defaultProvider = defaultProvider.trim().toLowerCase();
        this.providersByName = providers.stream()
                .collect(java.util.stream.Collectors.toMap(
                        provider -> provider.providerName().toLowerCase(),
                        provider -> provider
                ));

        if (!providersByName.containsKey(this.defaultProvider)) {
            throw new IllegalStateException("Default market data provider '" + this.defaultProvider + "' not configured.");
        }
    }

    public MarketDataFetchResult fetchCandles(String symbol, LocalDate startDate, LocalDate endDate, String interval) {
        validateDateRange(startDate, endDate);

        CacheKey key = new CacheKey(symbol.trim().toUpperCase(), startDate, endDate, interval.toLowerCase());
        CacheEntry cached = cache.get(key);
        if (cached != null && !isExpired(cached.cachedAt())) {
            return new MarketDataFetchResult(symbol.trim().toUpperCase(), interval, cached.providerName(), true, cached.candles());
        }

        MarketDataProvider provider = providersByName.get(defaultProvider);
        List<Candle> candles = provider.fetchCandles(symbol, startDate, endDate, interval);

        for (int i = 0; i < candles.size(); i++) {
            candleValidationService.validateCandle(candles.get(i), "market data row " + (i + 1));
        }
        candleValidationService.validateChronologicalOrder(candles, "market data provider");

        CacheEntry newEntry = new CacheEntry(List.copyOf(candles), provider.providerName(), Instant.now());
        cache.put(key, newEntry);

        return new MarketDataFetchResult(
                symbol.trim().toUpperCase(),
                interval,
                provider.providerName(),
                false,
                newEntry.candles()
        );
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Both startDate and endDate are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate.");
        }
    }

    private boolean isExpired(Instant cachedAt) {
        return cachedAt.plus(CACHE_TTL).isBefore(Instant.now());
    }

    private record CacheKey(
            String symbol,
            LocalDate startDate,
            LocalDate endDate,
            String interval
    ) {
    }

    private record CacheEntry(
            List<Candle> candles,
            String providerName,
            Instant cachedAt
    ) {
    }
}
