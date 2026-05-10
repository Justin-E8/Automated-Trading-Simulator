package com.tradingsim.application;

import com.tradingsim.api.dto.BacktestRunResponse;
import com.tradingsim.domain.Candle;
import com.tradingsim.engine.EquityPoint;
import com.tradingsim.engine.SimulationEngine;
import com.tradingsim.engine.SimulationRequest;
import com.tradingsim.engine.SimulationResult;
import com.tradingsim.infrastructure.csv.CsvCandleService;
import com.tradingsim.infrastructure.csv.CsvPreviewSummary;
import com.tradingsim.strategy.StrategyConfig;
import com.tradingsim.strategy.StrategyFactory;
import com.tradingsim.strategy.TradingStrategy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application-level orchestration for simulation use cases.
 *
 * <p>This service keeps API/controller code thin by handling candle mapping,
 * CSV ingestion orchestration, strategy construction, and response shaping.</p>
 */
@Service
public class SimulationService {

    private final SimulationEngine simulationEngine;
    private final CsvCandleService csvCandleService;
    private final StrategyFactory strategyFactory;

    public SimulationService(
            SimulationEngine simulationEngine,
            CsvCandleService csvCandleService,
            StrategyFactory strategyFactory
    ) {
        this.simulationEngine = simulationEngine;
        this.csvCandleService = csvCandleService;
        this.strategyFactory = strategyFactory;
    }

    /**
     * Runs a backtest directly from an uploaded CSV file.
     */
    public BacktestRunResponse runBacktestFromCsv(
            MultipartFile file,
            String symbol,
            StrategyConfig strategyConfig,
            BigDecimal initialCash,
            long quantityPerTrade,
            BigDecimal feeBps
    ) {
        List<Candle> candles = csvCandleService.parseCandles(file);
        return runBacktestWithCandles(
                symbol,
                strategyConfig,
                initialCash,
                quantityPerTrade,
                feeBps,
                candles
        );
    }

    /**
     * Parses and summarizes an uploaded CSV without executing a strategy.
     */
    public CsvPreviewSummary previewCsv(MultipartFile file) {
        return csvCandleService.preview(file);
    }

    private BacktestRunResponse runBacktestWithCandles(
            String symbol,
            StrategyConfig strategyConfig,
            BigDecimal initialCash,
            long quantityPerTrade,
            BigDecimal feeBps,
            List<Candle> candles
    ) {
        TradingStrategy selectedStrategy = strategyFactory.create(strategyConfig);

        SimulationRequest simulationRequest = new SimulationRequest(
                symbol,
                initialCash,
                quantityPerTrade,
                feeBps,
                candles
        );

        SimulationResult result = simulationEngine.run(simulationRequest, selectedStrategy);

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

    private BacktestRunResponse.EquityPointDto toEquityPointDto(EquityPoint point) {
        return new BacktestRunResponse.EquityPointDto(point.timestamp(), point.equity());
    }
}
