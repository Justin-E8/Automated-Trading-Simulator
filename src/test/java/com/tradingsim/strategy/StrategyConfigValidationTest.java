package com.tradingsim.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StrategyConfigValidationTest {

    @Test
    void smaConfig_rejectsInvalidWindowOrdering() {
        assertThatThrownBy(() -> new SmaCrossConfig(5, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shortWindow < longWindow");
    }

    @Test
    void meanReversionConfig_rejectsNonPositiveThreshold() {
        assertThatThrownBy(() -> new MeanReversionConfig(10, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("meanReversionThresholdPct > 0");
    }

    @Test
    void smaSweepRange_rejectsNonPositiveStep() {
        assertThatThrownBy(() -> new SmaSweepRange(2, 6, 0, 5, 20, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("steps must be > 0");
    }

    @Test
    void meanReversionSweepRange_rejectsDescendingThresholdRange() {
        assertThatThrownBy(() -> new MeanReversionSweepRange(
                5,
                10,
                1,
                new BigDecimal("2.0"),
                new BigDecimal("1.0"),
                new BigDecimal("0.5")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be <=");
    }
}
