package com.tradingsim.application;

import com.tradingsim.api.dto.BacktestRunResponse;
import com.tradingsim.api.dto.RunComparisonResponse;
import com.tradingsim.api.dto.RunHistoryResponse;
import com.tradingsim.api.dto.RunSummaryResponse;
import com.tradingsim.domain.Candle;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        SimulationRunEntity run = loadRun(runId);
        return toBacktestRunResponse(run);
    }

    /**
     * Lists saved runs with optional symbol/strategy filters.
     */
    @Transactional(readOnly = true)
    public RunHistoryResponse listRuns(int page, int size, String symbolFilter, String strategyFilter) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100.");
        }
        String symbol = symbolFilter == null ? "" : symbolFilter.trim();
        String strategy = strategyFilter == null ? "" : strategyFilter.trim();

        Page<SimulationRunEntity> runs = simulationRunRepository.findAllBySymbolContainingIgnoreCaseAndStrategyNameContainingIgnoreCase(
                symbol,
                strategy,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return new RunHistoryResponse(
                runs.getNumber(),
                runs.getSize(),
                runs.getTotalElements(),
                runs.getTotalPages(),
                runs.getContent().stream().map(this::toRunSummary).toList()
        );
    }

    /**
     * Compares two stored runs and returns metric deltas (right - left).
     */
    @Transactional(readOnly = true)
    public RunComparisonResponse compareRuns(long leftRunId, long rightRunId) {
        if (leftRunId == rightRunId) {
            throw new IllegalArgumentException("leftRunId and rightRunId must be different.");
        }
        SimulationRunEntity leftRun = loadRun(leftRunId);
        SimulationRunEntity rightRun = loadRun(rightRunId);

        return new RunComparisonResponse(
                toRunSummary(leftRun),
                toRunSummary(rightRun),
                new RunComparisonResponse.DeltaMetrics(
                        rightRun.getEndingEquity().subtract(leftRun.getEndingEquity()).doubleValue(),
                        rightRun.getTotalReturnPct() - leftRun.getTotalReturnPct(),
                        rightRun.getMaxDrawdownPct() - leftRun.getMaxDrawdownPct(),
                        rightRun.getSharpeRatio() - leftRun.getSharpeRatio(),
                        rightRun.getWinRatePct() - leftRun.getWinRatePct(),
                        rightRun.getTradeCount() - leftRun.getTradeCount()
                )
        );
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
        return toBacktestRunResponse(persistedRun);
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

    private BacktestRunResponse toBacktestRunResponse(SimulationRunEntity run) {
        return new BacktestRunResponse(
                run.getId(),
                run.getCreatedAt(),
                run.getSymbol(),
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

    private RunSummaryResponse toRunSummary(SimulationRunEntity run) {
        return new RunSummaryResponse(
                run.getId(),
                run.getCreatedAt(),
                run.getSymbol(),
                run.getStrategyName(),
                run.getStartingCash(),
                run.getEndingEquity(),
                run.getTotalReturnPct(),
                run.getMaxDrawdownPct(),
                run.getSharpeRatio(),
                run.getWinRatePct(),
                run.getTradeCount()
        );
    }

    private SimulationRunEntity loadRun(long runId) {
        return simulationRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Simulation run not found for id=" + runId));
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

    private BacktestRunResponse.EquityPointDto toEquityPointDto(SimulationEquityPointEntity point) {
        return new BacktestRunResponse.EquityPointDto(point.getTimestamp(), point.getEquity());
    }
}
