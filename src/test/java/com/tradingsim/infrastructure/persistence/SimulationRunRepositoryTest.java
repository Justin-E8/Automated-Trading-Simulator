package com.tradingsim.infrastructure.persistence;

import com.tradingsim.domain.OrderSide;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SimulationRunRepositoryTest {

    @Autowired
    private SimulationRunRepository simulationRunRepository;

    @Test
    void save_persistsRunWithTradesAndEquityPoints() {
        SimulationRunEntity run = new SimulationRunEntity(
                "AAPL",
                "SMA(3,5)",
                new BigDecimal("10000.0000"),
                new BigDecimal("10200.0000"),
                2.0,
                1.5,
                1.2,
                60.0,
                2
        );
        run.addTrade(new SimulationTradeEntity(
                LocalDateTime.parse("2025-01-01T09:30:00"),
                "AAPL",
                OrderSide.BUY,
                10,
                new BigDecimal("100.0000"),
                new BigDecimal("0.5000"),
                BigDecimal.ZERO
        ));
        run.addTrade(new SimulationTradeEntity(
                LocalDateTime.parse("2025-01-02T09:30:00"),
                "AAPL",
                OrderSide.SELL,
                10,
                new BigDecimal("102.0000"),
                new BigDecimal("0.5000"),
                new BigDecimal("19.0000")
        ));
        run.addEquityPoint(new SimulationEquityPointEntity(
                LocalDateTime.parse("2025-01-01T09:30:00"),
                new BigDecimal("9999.5000")
        ));
        run.addEquityPoint(new SimulationEquityPointEntity(
                LocalDateTime.parse("2025-01-02T09:30:00"),
                new BigDecimal("10200.0000")
        ));

        SimulationRunEntity saved = simulationRunRepository.save(run);

        SimulationRunEntity loaded = simulationRunRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getTrades()).hasSize(2);
        assertThat(loaded.getEquityPoints()).hasSize(2);
        assertThat(loaded.getEndingEquity()).isEqualByComparingTo("10200.0000");
    }
}
