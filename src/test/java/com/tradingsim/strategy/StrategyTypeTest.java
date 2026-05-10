package com.tradingsim.strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StrategyTypeTest {

    @Test
    void fromApiValue_resolvesKnownValues() {
        assertThat(StrategyType.fromApiValue("sma-cross")).isEqualTo(StrategyType.SMA_CROSS);
        assertThat(StrategyType.fromApiValue("MEAN-REVERSION")).isEqualTo(StrategyType.MEAN_REVERSION);
    }

    @Test
    void fromApiValue_rejectsUnknownValues() {
        assertThatThrownBy(() -> StrategyType.fromApiValue("random"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported strategy");
    }

    @Test
    void fromApiValue_rejectsBlankValue() {
        assertThatThrownBy(() -> StrategyType.fromApiValue(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Strategy is required");
    }
}
