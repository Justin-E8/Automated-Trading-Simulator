package com.tradingsim.engine;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EquityPoint(
        LocalDateTime timestamp,
        BigDecimal equity
) {
}
