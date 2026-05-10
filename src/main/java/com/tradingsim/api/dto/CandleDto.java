package com.tradingsim.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API-facing candle representation used in CSV preview responses.
 */
public record CandleDto(
        @NotNull LocalDateTime timestamp,
        @NotNull @Positive BigDecimal open,
        @NotNull @Positive BigDecimal high,
        @NotNull @Positive BigDecimal low,
        @NotNull @Positive BigDecimal close,
        @Positive long volume
) {
}
