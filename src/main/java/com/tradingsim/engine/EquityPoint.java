package com.tradingsim.engine;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Point on the simulated equity curve at a specific timestamp.
 *
 * @param timestamp candle timestamp
 * @param equity    total account equity at that timestamp
 */
public record EquityPoint(
        LocalDateTime timestamp,
        BigDecimal equity
) {
}
