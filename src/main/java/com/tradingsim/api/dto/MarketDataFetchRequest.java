package com.tradingsim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MarketDataFetchRequest(
        @NotBlank String symbol,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank String interval
) {
}
