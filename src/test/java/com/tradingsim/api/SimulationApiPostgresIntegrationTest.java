package com.tradingsim.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingsim.infrastructure.persistence.SimulationRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SimulationApiPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimulationRunRepository simulationRunRepository;

    @Test
    void runBacktest_persistsAndLoadsRunUsingPostgres() throws Exception {
        MvcResult backtestResult = mockMvc.perform(multipart("/api/v1/simulations/csv/backtest")
                        .file("file", csvContent().getBytes())
                        .param("symbol", "PGTEST")
                        .param("strategy", "sma-cross")
                        .param("initialCash", "10000")
                        .param("quantityPerTrade", "5")
                        .param("feeBps", "2")
                        .param("slippageBps", "0")
                        .param("maxPositionSize", "0")
                        .param("maxHoldingCandles", "0")
                        .param("stopLossPct", "0")
                        .param("takeProfitPct", "0")
                        .param("shortWindow", "3")
                        .param("longWindow", "6")
                        .param("meanReversionWindow", "8")
                        .param("meanReversionThresholdPct", "1.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isNumber())
                .andExpect(jsonPath("$.symbol").value("PGTEST"))
                .andReturn();

        JsonNode backtestBody = objectMapper.readTree(backtestResult.getResponse().getContentAsString());
        long runId = backtestBody.get("runId").asLong();

        mockMvc.perform(get("/api/v1/simulations/runs/{runId}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.symbol").value("PGTEST"))
                .andExpect(jsonPath("$.strategyName").exists())
                .andExpect(jsonPath("$.equityCurve").isArray());

        assertThat(simulationRunRepository.count()).isEqualTo(1);
    }

    private String csvContent() {
        StringBuilder builder = new StringBuilder("timestamp,open,high,low,close,volume\n");
        LocalDate start = LocalDate.parse("2025-01-01");
        for (int i = 0; i < 50; i++) {
            double close = 100 + i * 0.4 + Math.sin(i / 3.0);
            builder.append(start.plusDays(i)).append("T09:30:00,")
                    .append(String.format(Locale.US, "%.2f", close - 0.2)).append(",")
                    .append(String.format(Locale.US, "%.2f", close + 0.4)).append(",")
                    .append(String.format(Locale.US, "%.2f", close - 0.8)).append(",")
                    .append(String.format(Locale.US, "%.2f", close)).append(",")
                    .append(1_000 + i)
                    .append('\n');
        }
        return builder.toString();
    }
}
