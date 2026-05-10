package com.tradingsim.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StrategyFactoryTest {

    private final StrategyFactory strategyFactory = new StrategyFactory();

    @Test
    void create_returnsSmaCrossStrategyForSmaConfig() {
        TradingStrategy strategy = strategyFactory.create(new SmaCrossConfig(3, 5));

        assertThat(strategy).isInstanceOf(MovingAverageCrossStrategy.class);
        assertThat(strategy.name()).isEqualTo("SMA(3,5)");
    }

    @Test
    void create_returnsMeanReversionStrategyForMeanReversionConfig() {
        TradingStrategy strategy = strategyFactory.create(new MeanReversionConfig(8, new BigDecimal("1.5")));

        assertThat(strategy).isInstanceOf(MeanReversionStrategy.class);
        assertThat(strategy.name()).isEqualTo("MeanReversion(8,1.5%)");
    }
}
