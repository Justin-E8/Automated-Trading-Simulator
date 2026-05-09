package com.tradingsim.api;

import com.tradingsim.api.dto.BacktestRunRequest;
import com.tradingsim.api.dto.BacktestRunResponse;
import com.tradingsim.api.dto.CandleDto;
import com.tradingsim.api.dto.CsvPreviewResponse;
import com.tradingsim.api.dto.MarketDataBacktestRequest;
import com.tradingsim.api.dto.MarketDataFetchRequest;
import com.tradingsim.api.dto.MarketDataFetchResponse;
import com.tradingsim.application.SimulationService;
import com.tradingsim.infrastructure.csv.CsvPreviewSummary;
import com.tradingsim.infrastructure.marketdata.MarketDataFetchResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@Validated
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

    @PostMapping("/csv/backtest")
    public BacktestRunResponse runBacktestFromCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("symbol") @NotBlank String symbol,
            @RequestParam("initialCash") @DecimalMin("100.00") BigDecimal initialCash,
            @RequestParam("quantityPerTrade") @Min(1) long quantityPerTrade,
            @RequestParam("feeBps") @DecimalMin("0.0") BigDecimal feeBps,
            @RequestParam("shortWindow") @Min(2) int shortWindow,
            @RequestParam("longWindow") @Min(3) int longWindow
    ) {
        return simulationService.runBacktestFromCsv(
                file,
                symbol,
                initialCash,
                quantityPerTrade,
                feeBps,
                shortWindow,
                longWindow
        );
    }

    @PostMapping("/market-data/fetch")
    public MarketDataFetchResponse fetchMarketData(@Valid @RequestBody MarketDataFetchRequest request) {
        MarketDataFetchResult result = simulationService.fetchMarketData(
                request.symbol(),
                request.startDate(),
                request.endDate(),
                request.interval()
        );
        return toMarketDataFetchResponse(result);
    }

    @PostMapping("/market-data/backtest")
    public BacktestRunResponse runBacktestFromMarketData(@Valid @RequestBody MarketDataBacktestRequest request) {
        return simulationService.runBacktestFromMarketData(request);
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

    private MarketDataFetchResponse toMarketDataFetchResponse(MarketDataFetchResult result) {
        List<CandleDto> sample = result.candles().stream()
                .limit(5)
                .map(candle -> new CandleDto(
                        candle.timestamp(),
                        candle.open(),
                        candle.high(),
                        candle.low(),
                        candle.close(),
                        candle.volume()
                ))
                .toList();

        return new MarketDataFetchResponse(
                result.symbol(),
                result.interval(),
                result.provider(),
                result.cached(),
                result.candles().size(),
                result.candles().get(0).timestamp(),
                result.candles().get(result.candles().size() - 1).timestamp(),
                sample
        );
    }
}
