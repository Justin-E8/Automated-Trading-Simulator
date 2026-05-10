package com.tradingsim.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable trade event emitted by the simulation engine.
 *
 * @param timestamp   execution timestamp
 * @param symbol      traded symbol
 * @param side        buy or sell direction
 * @param quantity    number of shares executed
 * @param price       execution price
 * @param fee         transaction fee charged
 * @param realizedPnl realized profit/loss (non-zero on closing sells)
 */
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
