package com.tradingsim.application;

import com.tradingsim.domain.Candle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CandleValidationService {

    public void validateCandle(Candle candle, String context) {
        requirePositive(candle.open(), "open", context);
        requirePositive(candle.high(), "high", context);
        requirePositive(candle.low(), "low", context);
        requirePositive(candle.close(), "close", context);
        requireNonNegative(candle.volume(), "volume", context);

        if (candle.low().compareTo(candle.high()) > 0) {
            throw new IllegalArgumentException("Invalid high/low values at " + context + ".");
        }
        if (candle.high().compareTo(candle.open().max(candle.close())) < 0
                || candle.low().compareTo(candle.open().min(candle.close())) > 0) {
            throw new IllegalArgumentException("OHLC consistency check failed at " + context + ".");
        }
    }

    public void validateChronologicalOrder(List<Candle> candles, String source) {
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("No candles found from " + source + ".");
        }

        LocalDateTime previous = null;
        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            if (previous != null && !candle.timestamp().isAfter(previous)) {
                throw new IllegalArgumentException("Invalid timestamp order at " + source
                        + " row " + (i + 1) + ". Timestamps must be strictly increasing.");
            }
            previous = candle.timestamp();
        }
    }

    private void requirePositive(BigDecimal value, String fieldName, String context) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expected positive '" + fieldName + "' at " + context + ".");
        }
    }

    private void requireNonNegative(long value, String fieldName, String context) {
        if (value < 0) {
            throw new IllegalArgumentException("Expected non-negative '" + fieldName + "' at " + context + ".");
        }
    }
}
