package com.tradingsim.infrastructure.marketdata;

import com.tradingsim.application.CandleValidationService;
import com.tradingsim.domain.Candle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketDataServiceTest {

    @Test
    void fetchCandles_usesCacheForSameRequest() {
        FakeProvider provider = new FakeProvider(validCandles());
        MarketDataService service = new MarketDataService(List.of(provider), new CandleValidationService(), "alphavantage");

        MarketDataFetchResult first = service.fetchCandles("AAPL", date("2025-01-01"), date("2025-01-04"), "1d");
        MarketDataFetchResult second = service.fetchCandles("AAPL", date("2025-01-01"), date("2025-01-04"), "1d");

        assertThat(first.cached()).isFalse();
        assertThat(second.cached()).isTrue();
        assertThat(provider.calls).isEqualTo(1);
    }

    @Test
    void fetchCandles_invalidChronologicalOrder_throwsValidationError() {
        List<Candle> invalid = new ArrayList<>();
        invalid.add(candle("2025-01-02T09:30:00", "101", "102", "100", "101.4", 1000));
        invalid.add(candle("2025-01-01T09:30:00", "100", "101", "99", "100.2", 1000));

        FakeProvider provider = new FakeProvider(invalid);
        MarketDataService service = new MarketDataService(List.of(provider), new CandleValidationService(), "alphavantage");

        assertThatThrownBy(() -> service.fetchCandles("AAPL", date("2025-01-01"), date("2025-01-02"), "1d"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictly increasing");
    }

    private List<Candle> validCandles() {
        return List.of(
                candle("2025-01-01T09:30:00", "100", "101", "99", "100.2", 1000),
                candle("2025-01-02T09:30:00", "100.2", "102", "100", "101.4", 1100),
                candle("2025-01-03T09:30:00", "101.4", "103", "101", "102.1", 1200),
                candle("2025-01-04T09:30:00", "102.1", "103.2", "101.8", "102.4", 900)
        );
    }

    private Candle candle(String timestamp, String open, String high, String low, String close, long volume) {
        return new Candle(
                LocalDateTime.parse(timestamp),
                new BigDecimal(open),
                new BigDecimal(high),
                new BigDecimal(low),
                new BigDecimal(close),
                volume
        );
    }

    private LocalDate date(String value) {
        return LocalDate.parse(value);
    }

    private static final class FakeProvider implements MarketDataProvider {
        private final List<Candle> candles;
        private int calls = 0;

        private FakeProvider(List<Candle> candles) {
            this.candles = candles;
        }

        @Override
        public String providerName() {
            return "alphavantage";
        }

        @Override
        public List<Candle> fetchCandles(String symbol, LocalDate startDate, LocalDate endDate, String interval) {
            calls++;
            return candles;
        }
    }
}
