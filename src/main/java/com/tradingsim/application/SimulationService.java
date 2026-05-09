package com.tradingsim.application;

import com.tradingsim.api.dto.BacktestRunRequest;
import com.tradingsim.api.dto.BacktestRunResponse;
import com.tradingsim.api.dto.MarketDataBacktestRequest;
import com.tradingsim.domain.Candle;
import com.tradingsim.engine.EquityPoint;
import com.tradingsim.engine.SimulationEngine;
import com.tradingsim.engine.SimulationRequest;
import com.tradingsim.engine.SimulationResult;
import com.tradingsim.infrastructure.csv.CsvCandleService;
import com.tradingsim.infrastructure.csv.CsvPreviewSummary;
import com.tradingsim.infrastructure.marketdata.MarketDataFetchResult;
import com.tradingsim.infrastructure.marketdata.MarketDataService;
import com.tradingsim.strategy.MovingAverageCrossStrategy;
import com.tradingsim.strategy.TradingStrategy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class SimulationService {

    private final SimulationEngine simulationEngine;
    private final CsvCandleService csvCandleService;
    private final MarketDataService marketDataService;

    public SimulationService(
            SimulationEngine simulationEngine,
            CsvCandleService csvCandleService,
            MarketDataService marketDataService
    ) {
        this.simulationEngine = simulationEngine;
        this.csvCandleService = csvCandleService;
        this.marketDataService = marketDataService;
    }

    public BacktestRunResponse runBacktest(BacktestRunRequest request) {
        List<Candle> candles = request.candles().stream()
                .map(c -> new Candle(c.timestamp(), c.open(), c.high(), c.low(), c.close(), c.volume()))
                .toList();

        return runBacktestWithCandles(
                request.symbol(),
                request.initialCash(),
                request.quantityPerTrade(),
                request.feeBps(),
                request.shortWindow(),
                request.longWindow(),
                candles
        );
    }

    public BacktestRunResponse runBacktestFromCsv(
            MultipartFile file,
            String symbol,
            BigDecimal initialCash,
            long quantityPerTrade,
            BigDecimal feeBps,
            int shortWindow,
            int longWindow
    ) {
        List<Candle> candles = csvCandleService.parseCandles(file);
        return runBacktestWithCandles(symbol, initialCash, quantityPerTrade, feeBps, shortWindow, longWindow, candles);
    }

    public CsvPreviewSummary previewCsv(MultipartFile file) {
        return csvCandleService.preview(file);
    }

    public MarketDataFetchResult fetchMarketData(
            String symbol,
            LocalDate startDate,
            LocalDate endDate,
            String interval
    ) {
        return marketDataService.fetchCandles(symbol, startDate, endDate, interval);
    }

    public BacktestRunResponse runBacktestFromMarketData(MarketDataBacktestRequest request) {
        MarketDataFetchResult marketData = marketDataService.fetchCandles(
                request.symbol(),
                request.startDate(),
                request.endDate(),
                request.interval()
        );
        return runBacktestWithCandles(
                request.symbol(),
                request.initialCash(),
                request.quantityPerTrade(),
                request.feeBps(),
                request.shortWindow(),
                request.longWindow(),
                marketData.candles()
        );
    }

    private BacktestRunResponse runBacktestWithCandles(
            String symbol,
            BigDecimal initialCash,
            long quantityPerTrade,
            BigDecimal feeBps,
            int shortWindow,
            int longWindow,
            List<Candle> candles
    ) {
        validateWindows(shortWindow, longWindow);

        SimulationRequest simulationRequest = new SimulationRequest(
                symbol,
                initialCash,
                quantityPerTrade,
                feeBps,
                candles
        );

        TradingStrategy strategy = new MovingAverageCrossStrategy(shortWindow, longWindow);
        SimulationResult result = simulationEngine.run(simulationRequest, strategy);

        return new BacktestRunResponse(
                result.strategyName(),
                result.startingCash(),
                result.endingEquity(),
                new BacktestRunResponse.MetricsDto(
                        result.metrics().totalReturnPct(),
                        result.metrics().maxDrawdownPct(),
                        result.metrics().sharpeRatio(),
                        result.metrics().winRatePct(),
                        result.metrics().tradeCount()
                ),
                result.trades().stream().map(trade ->
                        new BacktestRunResponse.TradeDto(
                                trade.timestamp(),
                                trade.symbol(),
                                trade.side(),
                                trade.quantity(),
                                trade.price(),
                                trade.fee(),
                                trade.realizedPnl()
                        )
                ).toList(),
                result.equityCurve().stream().map(this::toEquityPointDto).toList()
        );
    }

    private void validateWindows(int shortWindow, int longWindow) {
        if (shortWindow >= longWindow) {
            throw new IllegalArgumentException("Expected shortWindow < longWindow.");
        }
    }

    private BacktestRunResponse.EquityPointDto toEquityPointDto(EquityPoint point) {
        return new BacktestRunResponse.EquityPointDto(point.timestamp(), point.equity());
    }
}
