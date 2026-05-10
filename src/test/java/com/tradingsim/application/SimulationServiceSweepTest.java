package com.tradingsim.application;

import com.tradingsim.api.dto.ParameterSweepResponse;
import com.tradingsim.api.dto.SweepObjective;
import com.tradingsim.infrastructure.persistence.SimulationRunRepository;
import com.tradingsim.strategy.MeanReversionSweepRange;
import com.tradingsim.strategy.SmaSweepRange;
import com.tradingsim.strategy.StrategyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SimulationServiceSweepTest {

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private SimulationRunRepository simulationRunRepository;

    @Test
    void runParameterSweepFromCsv_returnsRankedSmaCombinationsWithoutPersistingRuns() {
        long beforeCount = simulationRunRepository.count();

        ParameterSweepResponse response = simulationService.runParameterSweepFromCsv(
                csvFile(),
                "TEST",
                StrategyType.SMA_CROSS,
                new BigDecimal("10000"),
                5,
                new BigDecimal("5.0"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                SweepObjective.TOTAL_RETURN_PCT,
                5,
                new SmaSweepRange(2, 4, 1, 4, 6, 1),
                new MeanReversionSweepRange(5, 6, 1, new BigDecimal("0.5"), new BigDecimal("1.0"), new BigDecimal("0.5"))
        );

        assertThat(response.evaluatedCombinations()).isEqualTo(8);
        assertThat(response.returnedCombinations()).isEqualTo(5);
        assertThat(response.bestResult()).isNotNull();
        assertThat(response.results()).hasSize(5);
        assertThat(response.results().get(0).rank()).isEqualTo(1);
        assertThat(response.results().get(0).objectiveScore()).isGreaterThanOrEqualTo(response.results().get(1).objectiveScore());
        assertThat(simulationRunRepository.count()).isEqualTo(beforeCount);
    }

    @Test
    void runParameterSweepFromCsv_supportsMeanReversionGrid() {
        ParameterSweepResponse response = simulationService.runParameterSweepFromCsv(
                csvFile(),
                "TEST",
                StrategyType.MEAN_REVERSION,
                new BigDecimal("10000"),
                5,
                new BigDecimal("5.0"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                SweepObjective.SHARPE_RATIO,
                10,
                new SmaSweepRange(2, 4, 1, 4, 6, 1),
                new MeanReversionSweepRange(5, 6, 1, new BigDecimal("0.5"), new BigDecimal("1.5"), new BigDecimal("0.5"))
        );

        assertThat(response.evaluatedCombinations()).isEqualTo(6);
        assertThat(response.results()).isNotEmpty();
        assertThat(response.bestResult().parameters().meanReversionWindow()).isNotNull();
        assertThat(response.bestResult().parameters().meanReversionThresholdPct()).isNotNull();
    }

    private MockMultipartFile csvFile() {
        StringBuilder builder = new StringBuilder("timestamp,open,high,low,close,volume\n");
        LocalDate start = LocalDate.parse("2025-01-01");
        for (int i = 0; i < 40; i++) {
            double base = 100 + (i * 0.6);
            double wave = (i % 6 == 0) ? -2.0 : (i % 7 == 0 ? 1.8 : 0.0);
            double close = base + wave;
            builder.append(start.plusDays(i)).append("T09:30:00,")
                    .append(String.format(Locale.US, "%.2f", close - 0.5)).append(",")
                    .append(String.format(Locale.US, "%.2f", close + 0.7)).append(",")
                    .append(String.format(Locale.US, "%.2f", close - 1.0)).append(",")
                    .append(String.format(Locale.US, "%.2f", close)).append(",")
                    .append(1000 + i)
                    .append("\n");
        }
        return new MockMultipartFile("file", "sweep.csv", "text/csv", builder.toString().getBytes());
    }
}
