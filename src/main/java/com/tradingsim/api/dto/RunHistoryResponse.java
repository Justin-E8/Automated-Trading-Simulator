package com.tradingsim.api.dto;

import java.util.List;

/**
 * Paged run-history payload with optional filter results.
 */
public record RunHistoryResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<RunSummaryResponse> runs
) {
}
