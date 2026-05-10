package com.tradingsim.application;

import com.tradingsim.api.dto.RunComparisonResponse;
import com.tradingsim.api.dto.RunHistoryResponse;
import com.tradingsim.infrastructure.persistence.SimulationRunEntity;
import com.tradingsim.infrastructure.persistence.SimulationRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SimulationServiceHistoryTest {

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private SimulationRunRepository simulationRunRepository;

    @Test
    void listRuns_filtersBySymbolAndStrategy() {
        simulationRunRepository.save(run("AAPL", "SMA(3,5)", "10000.0000", "10100.0000", 1.0, 0.5, 1.1, 55.0, 2));
        simulationRunRepository.save(run("MSFT", "MeanReversion(8,1.5%)", "10000.0000", "10300.0000", 3.0, 1.2, 1.3, 60.0, 3));

        RunHistoryResponse history = simulationService.listRuns(0, 10, "MSF", "mean");

        assertThat(history.totalElements()).isEqualTo(1);
        assertThat(history.runs()).hasSize(1);
        assertThat(history.runs().get(0).symbol()).isEqualTo("MSFT");
        assertThat(history.runs().get(0).strategyName()).startsWith("MeanReversion");
    }

    @Test
    void compareRuns_returnsRightMinusLeftDeltas() {
        SimulationRunEntity left = simulationRunRepository.save(
                run("AAPL", "SMA(3,5)", "10000.0000", "10050.0000", 0.5, 2.0, 1.0, 50.0, 4)
        );
        SimulationRunEntity right = simulationRunRepository.save(
                run("AAPL", "SMA(5,10)", "10000.0000", "10200.0000", 2.0, 1.5, 1.2, 60.0, 6)
        );

        RunComparisonResponse comparison = simulationService.compareRuns(left.getId(), right.getId());

        assertThat(comparison.delta().endingEquityDelta()).isEqualTo(150.0);
        assertThat(comparison.delta().totalReturnPctDelta()).isEqualTo(1.5);
        assertThat(comparison.delta().tradeCountDelta()).isEqualTo(2);
    }

    private SimulationRunEntity run(
            String symbol,
            String strategyName,
            String startingCash,
            String endingEquity,
            double totalReturnPct,
            double maxDrawdownPct,
            double sharpeRatio,
            double winRatePct,
            long tradeCount
    ) {
        return new SimulationRunEntity(
                symbol,
                strategyName,
                new BigDecimal(startingCash),
                new BigDecimal(endingEquity),
                totalReturnPct,
                maxDrawdownPct,
                sharpeRatio,
                winRatePct,
                tradeCount
        );
    }
}
