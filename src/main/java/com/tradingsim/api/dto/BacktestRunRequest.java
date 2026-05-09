package com.tradingsim.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record BacktestRunRequest(
        @NotBlank String symbol,
        @NotNull @DecimalMin("100.00") BigDecimal initialCash,
        @Min(1) long quantityPerTrade,
        @NotNull @DecimalMin("0.0") BigDecimal feeBps,
        @Min(2) int shortWindow,
        @Min(3) int longWindow,
        @NotEmpty List<@Valid CandleDto> candles
) {
}
