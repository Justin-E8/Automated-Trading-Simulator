package com.tradingsim.strategy;

/**
 * Inclusive integer range definition for SMA short/long sweep grids.
 */
public record SmaSweepRange(
        int shortWindowStart,
        int shortWindowEnd,
        int shortWindowStep,
        int longWindowStart,
        int longWindowEnd,
        int longWindowStep
) {
    public SmaSweepRange {
        if (shortWindowStart < 2 || shortWindowEnd < 2) {
            throw new IllegalArgumentException("shortWindow sweep bounds must be >= 2.");
        }
        if (longWindowStart < 3 || longWindowEnd < 3) {
            throw new IllegalArgumentException("longWindow sweep bounds must be >= 3.");
        }
        if (shortWindowStep <= 0 || longWindowStep <= 0) {
            throw new IllegalArgumentException("SMA sweep steps must be > 0.");
        }
        if (shortWindowStart > shortWindowEnd) {
            throw new IllegalArgumentException("shortWindowStart must be <= shortWindowEnd.");
        }
        if (longWindowStart > longWindowEnd) {
            throw new IllegalArgumentException("longWindowStart must be <= longWindowEnd.");
        }
    }
}
