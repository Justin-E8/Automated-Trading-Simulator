package com.tradingsim.api;

import com.tradingsim.api.dto.BacktestRunRequest;
import com.tradingsim.api.dto.BacktestRunResponse;
import com.tradingsim.api.dto.CandleDto;
import com.tradingsim.application.SimulationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/simulations")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/backtest")
    public BacktestRunResponse runBacktest(@Valid @RequestBody BacktestRunRequest request) {
        return simulationService.runBacktest(request);
    }

    @GetMapping("/sample-candles")
    public List<CandleDto> sampleCandles() {
        return List.of(
                candle("2025-01-01T09:30:00", "100.00"),
                candle("2025-01-02T09:30:00", "101.00"),
                candle("2025-01-03T09:30:00", "102.00"),
                candle("2025-01-04T09:30:00", "103.50"),
                candle("2025-01-05T09:30:00", "104.20"),
                candle("2025-01-06T09:30:00", "104.00"),
                candle("2025-01-07T09:30:00", "103.20"),
                candle("2025-01-08T09:30:00", "102.60"),
                candle("2025-01-09T09:30:00", "101.90"),
                candle("2025-01-10T09:30:00", "101.10"),
                candle("2025-01-11T09:30:00", "101.80"),
                candle("2025-01-12T09:30:00", "102.90"),
                candle("2025-01-13T09:30:00", "104.30"),
                candle("2025-01-14T09:30:00", "105.10"),
                candle("2025-01-15T09:30:00", "105.80")
        );
    }

    private CandleDto candle(String timestamp, String close) {
        BigDecimal closePrice = new BigDecimal(close);
        return new CandleDto(
                LocalDateTime.parse(timestamp),
                closePrice,
                closePrice,
                closePrice,
                closePrice,
                1000
        );
    }
}
