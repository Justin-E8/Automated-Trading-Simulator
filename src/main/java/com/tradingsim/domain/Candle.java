package com.tradingsim.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Candle(
        LocalDateTime timestamp,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {
}
