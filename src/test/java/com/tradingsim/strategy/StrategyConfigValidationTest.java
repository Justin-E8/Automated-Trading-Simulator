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
}
