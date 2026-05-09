package com.tradingsim.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Trade(
        LocalDateTime timestamp,
        String symbol,
        OrderSide side,
        long quantity,
        BigDecimal price,
        BigDecimal fee,
        BigDecimal realizedPnl
) {
}
