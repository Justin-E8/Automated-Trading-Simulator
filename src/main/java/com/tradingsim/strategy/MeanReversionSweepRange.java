package com.tradingsim.strategy;

import java.math.BigDecimal;

/**
 * Sweep ranges for mean-reversion window and threshold percentage.
 */
public record MeanReversionSweepRange(
        int windowStart,
        int windowEnd,
        int windowStep,
        BigDecimal thresholdStartPct,
        BigDecimal thresholdEndPct,
        BigDecimal thresholdStepPct
) {
    public MeanReversionSweepRange {
        if (windowStart < 2 || windowEnd < 2) {
            throw new IllegalArgumentException("meanReversionWindow sweep bounds must be >= 2.");
        }
        if (windowStep <= 0) {
            throw new IllegalArgumentException("meanReversionWindow step must be > 0.");
        }
        if (windowStart > windowEnd) {
            throw new IllegalArgumentException("meanReversionWindowStart must be <= meanReversionWindowEnd.");
        }
        if (thresholdStartPct == null || thresholdEndPct == null || thresholdStepPct == null) {
            throw new IllegalArgumentException("Mean reversion threshold sweep values are required.");
        }
        if (thresholdStartPct.compareTo(BigDecimal.ZERO) <= 0 || thresholdEndPct.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mean reversion threshold sweep bounds must be > 0.");
        }
        if (thresholdStepPct.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mean reversion threshold step must be > 0.");
        }
        if (thresholdStartPct.compareTo(thresholdEndPct) > 0) {
            throw new IllegalArgumentException("meanReversionThresholdStartPct must be <= meanReversionThresholdEndPct.");
        }
    }
}
