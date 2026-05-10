package com.tradingsim.api;

import com.tradingsim.api.dto.BacktestRunResponse;
import com.tradingsim.api.dto.CandleDto;
import com.tradingsim.api.dto.CsvPreviewResponse;
import com.tradingsim.api.dto.RunComparisonResponse;
import com.tradingsim.api.dto.RunHistoryResponse;
import com.tradingsim.application.SimulationService;
import com.tradingsim.infrastructure.csv.CsvPreviewSummary;
import com.tradingsim.strategy.MeanReversionConfig;
import com.tradingsim.strategy.SmaCrossConfig;
import com.tradingsim.strategy.StrategyConfig;
import com.tradingsim.strategy.StrategyType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/**
 * REST endpoints for CSV preview and backtest execution.
 */
@RestController
@Validated
@RequestMapping("/api/v1/simulations")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * Parses uploaded CSV and returns dataset summary + sample candles.
     */
    @PostMapping("/csv/preview")
    public CsvPreviewResponse previewCsv(@RequestParam("file") MultipartFile file) {
        CsvPreviewSummary preview = simulationService.previewCsv(file);
        return new CsvPreviewResponse(
                preview.candleCount(),
                preview.startTimestamp(),
                preview.endTimestamp(),
                preview.minClose(),
                preview.maxClose(),
                preview.sampleCandles().stream()
                        .map(candle -> new CandleDto(
                                candle.timestamp(),
                                candle.open(),
                                candle.high(),
                                candle.low(),
                                candle.close(),
                                candle.volume()
                        ))
                        .toList()
        );
    }

    /**
     * Runs a simulation directly from uploaded CSV data.
     */
    @PostMapping("/csv/backtest")
    public BacktestRunResponse runBacktestFromCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("symbol") @NotBlank String symbol,
            @RequestParam(name = "strategy", defaultValue = "sma-cross") @NotBlank String strategy,
            @RequestParam("initialCash") @DecimalMin("100.00") BigDecimal initialCash,
            @RequestParam("quantityPerTrade") @Min(1) long quantityPerTrade,
            @RequestParam("feeBps") @DecimalMin("0.0") BigDecimal feeBps,
            @RequestParam(name = "slippageBps", defaultValue = "0.0") @DecimalMin("0.0") BigDecimal slippageBps,
            @RequestParam(name = "stopLossPct", defaultValue = "0.0") @DecimalMin("0.0") BigDecimal stopLossPct,
            @RequestParam(name = "takeProfitPct", defaultValue = "0.0") @DecimalMin("0.0") BigDecimal takeProfitPct,
            @RequestParam(name = "maxPositionSize", defaultValue = "0") @Min(0) long maxPositionSize,
            @RequestParam(name = "maxHoldingCandles", defaultValue = "0") @Min(0) int maxHoldingCandles,
            @RequestParam("shortWindow") @Min(2) int shortWindow,
            @RequestParam("longWindow") @Min(3) int longWindow,
            @RequestParam(name = "meanReversionWindow", defaultValue = "10") @Min(2) int meanReversionWindow,
            @RequestParam(name = "meanReversionThresholdPct", defaultValue = "2.0")
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal meanReversionThresholdPct
    ) {
        StrategyConfig strategyConfig = toStrategyConfig(
                strategy,
                shortWindow,
                longWindow,
                meanReversionWindow,
                meanReversionThresholdPct
        );
        return simulationService.runBacktestFromCsv(
                file,
                symbol,
                strategyConfig,
                initialCash,
                quantityPerTrade,
                feeBps,
                slippageBps,
                stopLossPct,
                takeProfitPct,
                maxPositionSize,
                maxHoldingCandles
        );
    }

    /**
     * Retrieves a previously saved simulation run by ID.
     */
    @GetMapping("/runs/{runId}")
    public BacktestRunResponse getRunById(@PathVariable("runId") @Positive long runId) {
        return simulationService.getRunById(runId);
    }

    /**
     * Lists saved runs with optional symbol/strategy filtering.
     */
    @GetMapping("/runs")
    public RunHistoryResponse listRuns(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size,
            @RequestParam(name = "symbol", defaultValue = "") String symbol,
            @RequestParam(name = "strategy", defaultValue = "") String strategy
    ) {
        return simulationService.listRuns(page, size, symbol, strategy);
    }

    /**
     * Compares two runs and returns metric deltas (right - left).
     */
    @GetMapping("/runs/compare")
    public RunComparisonResponse compareRuns(
            @RequestParam("leftRunId") @Positive long leftRunId,
            @RequestParam("rightRunId") @Positive long rightRunId
    ) {
        return simulationService.compareRuns(leftRunId, rightRunId);
    }

    private StrategyConfig toStrategyConfig(
            String strategy,
            int shortWindow,
            int longWindow,
            int meanReversionWindow,
            BigDecimal meanReversionThresholdPct
    ) {
        StrategyType strategyType = StrategyType.fromApiValue(strategy);
        return switch (strategyType) {
            case SMA_CROSS -> new SmaCrossConfig(shortWindow, longWindow);
            case MEAN_REVERSION -> new MeanReversionConfig(meanReversionWindow, meanReversionThresholdPct);
        };
    }

}
