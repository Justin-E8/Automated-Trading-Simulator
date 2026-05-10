package com.tradingsim.application;

import com.tradingsim.api.dto.BacktestRunResponse;
import com.tradingsim.domain.Candle;
import com.tradingsim.engine.EquityPoint;
import com.tradingsim.engine.SimulationEngine;
import com.tradingsim.engine.SimulationRequest;
import com.tradingsim.engine.SimulationResult;
import com.tradingsim.infrastructure.csv.CsvCandleService;
import com.tradingsim.infrastructure.csv.CsvPreviewSummary;
import com.tradingsim.infrastructure.persistence.SimulationEquityPointEntity;
import com.tradingsim.infrastructure.persistence.SimulationRunEntity;
import com.tradingsim.infrastructure.persistence.SimulationRunRepository;
import com.tradingsim.infrastructure.persistence.SimulationTradeEntity;
import com.tradingsim.strategy.StrategyConfig;
import com.tradingsim.strategy.StrategyFactory;
import com.tradingsim.strategy.TradingStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final SimulationRunRepository simulationRunRepository;

    public SimulationService(
            SimulationEngine simulationEngine,
            CsvCandleService csvCandleService,
            StrategyFactory strategyFactory,
            SimulationRunRepository simulationRunRepository
    ) {
        this.simulationEngine = simulationEngine;
        this.csvCandleService = csvCandleService;
        this.strategyFactory = strategyFactory;
        this.simulationRunRepository = simulationRunRepository;
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
            BigDecimal feeBps,
            BigDecimal slippageBps,
            BigDecimal stopLossPct,
            BigDecimal takeProfitPct,
            long maxPositionSize,
            int maxHoldingCandles
    ) {
        List<Candle> candles = csvCandleService.parseCandles(file);
        return runBacktestWithCandles(
                symbol,
                strategyConfig,
                initialCash,
                quantityPerTrade,
                feeBps,
                slippageBps,
                stopLossPct,
                takeProfitPct,
                maxPositionSize,
                maxHoldingCandles,
                candles
        );
    }

    /**
     * Parses and summarizes an uploaded CSV without executing a strategy.
     */
    public CsvPreviewSummary previewCsv(MultipartFile file) {
        return csvCandleService.preview(file);
    }

    /**
     * Fetches a stored simulation run by ID.
     */
    @Transactional(readOnly = true)
    public BacktestRunResponse getRunById(long runId) {
        SimulationRunEntity run = simulationRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Simulation run not found for id=" + runId));
        return toBacktestRunResponse(run);
    }

    private BacktestRunResponse runBacktestWithCandles(
            String symbol,
            StrategyConfig strategyConfig,
            BigDecimal initialCash,
            long quantityPerTrade,
            BigDecimal feeBps,
            BigDecimal slippageBps,
            BigDecimal stopLossPct,
            BigDecimal takeProfitPct,
            long maxPositionSize,
            int maxHoldingCandles,
            List<Candle> candles
    ) {
        TradingStrategy selectedStrategy = strategyFactory.create(strategyConfig);

        SimulationRequest simulationRequest = new SimulationRequest(
                symbol,
                initialCash,
                quantityPerTrade,
                feeBps,
                slippageBps,
                stopLossPct,
                takeProfitPct,
                maxPositionSize,
                maxHoldingCandles,
                candles
        );

        SimulationResult result = simulationEngine.run(simulationRequest, selectedStrategy);
        SimulationRunEntity persistedRun = persistRun(result, symbol);
        return toBacktestRunResponse(persistedRun.getId(), result);
    }

    private SimulationRunEntity persistRun(SimulationResult result, String symbol) {
        SimulationRunEntity run = new SimulationRunEntity(
                symbol,
                result.strategyName(),
                result.startingCash(),
                result.endingEquity(),
                result.metrics().totalReturnPct(),
                result.metrics().maxDrawdownPct(),
                result.metrics().sharpeRatio(),
                result.metrics().winRatePct(),
                result.metrics().tradeCount()
        );

        result.trades().forEach(trade -> run.addTrade(new SimulationTradeEntity(
                trade.timestamp(),
                trade.symbol(),
                trade.side(),
                trade.quantity(),
                trade.price(),
                trade.fee(),
                trade.realizedPnl()
        )));
        result.equityCurve().forEach(point -> run.addEquityPoint(
                new SimulationEquityPointEntity(point.timestamp(), point.equity())
        ));
        return simulationRunRepository.save(run);
    }

    private BacktestRunResponse toBacktestRunResponse(Long runId, SimulationResult result) {
        return new BacktestRunResponse(
                runId,
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

    private BacktestRunResponse toBacktestRunResponse(SimulationRunEntity run) {
        return new BacktestRunResponse(
                run.getId(),
                run.getStrategyName(),
                run.getStartingCash(),
                run.getEndingEquity(),
                new BacktestRunResponse.MetricsDto(
                        run.getTotalReturnPct(),
                        run.getMaxDrawdownPct(),
                        run.getSharpeRatio(),
                        run.getWinRatePct(),
                        run.getTradeCount()
                ),
                run.getTrades().stream().map(this::toTradeDto).toList(),
                run.getEquityPoints().stream().map(this::toEquityPointDto).toList()
        );
    }

    private BacktestRunResponse.TradeDto toTradeDto(SimulationTradeEntity trade) {
        return new BacktestRunResponse.TradeDto(
                trade.getTimestamp(),
                trade.getSymbol(),
                trade.getSide(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getFee(),
                trade.getRealizedPnl()
        );
    }

    private BacktestRunResponse.EquityPointDto toEquityPointDto(EquityPoint point) {
        return new BacktestRunResponse.EquityPointDto(point.timestamp(), point.equity());
    }

    private BacktestRunResponse.EquityPointDto toEquityPointDto(SimulationEquityPointEntity point) {
        return new BacktestRunResponse.EquityPointDto(point.getTimestamp(), point.getEquity());
    }
}
