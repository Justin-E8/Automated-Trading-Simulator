package com.tradingsim.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable OHLCV market candle used as simulation input.
 *
 * @param timestamp candle close timestamp
 * @param open      opening price
 * @param high      highest traded price
 * @param low       lowest traded price
 * @param close     closing price
 * @param volume    traded volume
 */
public record Candle(
        LocalDateTime timestamp,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {
}
