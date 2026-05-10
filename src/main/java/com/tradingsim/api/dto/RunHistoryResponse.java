package com.tradingsim.api.dto;

import java.util.List;

public record RunHistoryResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<RunSummaryResponse> runs
) {
}
