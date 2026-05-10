package com.tradingsim.application;

import com.tradingsim.api.dto.BacktestRunResponse;
import com.tradingsim.api.dto.ParameterSweepResponse;
import com.tradingsim.api.dto.RunComparisonResponse;
import com.tradingsim.api.dto.RunHistoryResponse;
import com.tradingsim.api.dto.RunSummaryResponse;
import com.tradingsim.api.dto.SweepObjective;
import com.tradingsim.domain.Candle;
import com.tradingsim.engine.SimulationEngine;
import com.tradingsim.engine.PerformanceMetrics;
import com.tradingsim.engine.SimulationRequest;
import com.tradingsim.engine.SimulationResult;
import com.tradingsim.infrastructure.csv.CsvCandleService;
import com.tradingsim.infrastructure.csv.CsvPreviewSummary;
import com.tradingsim.infrastructure.persistence.SimulationEquityPointEntity;
import com.tradingsim.infrastructure.persistence.SimulationRunEntity;
import com.tradingsim.infrastructure.persistence.SimulationRunRepository;
import com.tradingsim.infrastructure.persistence.SimulationTradeEntity;
import com.tradingsim.strategy.MeanReversionConfig;
import com.tradingsim.strategy.MeanReversionSweepRange;
import com.tradingsim.strategy.SmaCrossConfig;
import com.tradingsim.strategy.SmaSweepRange;
import com.tradingsim.strategy.StrategyConfig;
import com.tradingsim.strategy.StrategyFactory;
import com.tradingsim.strategy.StrategyType;
import com.tradingsim.strategy.TradingStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Application-level orchestration for simulation use cases.
 *
 * <p>This service keeps API/controller code thin by handling candle mapping,
 * CSV ingestion orchestration, strategy construction, and response shaping.</p>
 */
@Service
public class SimulationService {
    private static final int MAX_SWEEP_COMBINATIONS = 2000;

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
     * Runs a parameter sweep and returns ranked strategy configurations.
     */
    public ParameterSweepResponse runParameterSweepFromCsv(
            MultipartFile file,
            String symbol,
            StrategyType strategyType,
            BigDecimal initialCash,
            long quantityPerTrade,
            BigDecimal feeBps,
            BigDecimal slippageBps,
            BigDecimal stopLossPct,
            BigDecimal takeProfitPct,
            long maxPositionSize,
            int maxHoldingCandles,
            SweepObjective optimizeFor,
            int maxResults,
            SmaSweepRange smaRange,
            MeanReversionSweepRange meanReversionRange
    ) {
        List<Candle> candles = csvCandleService.parseCandles(file);
        List<StrategyConfig> sweepConfigs = buildSweepConfigs(strategyType, smaRange, meanReversionRange);
        if (sweepConfigs.isEmpty()) {
            throw new IllegalArgumentException("Parameter sweep produced no valid combinations.");
        }
        if (sweepConfigs.size() > MAX_SWEEP_COMBINATIONS) {
            throw new IllegalArgumentException(
                    "Parameter sweep produced " + sweepConfigs.size() + " combinations. Reduce ranges to <= " + MAX_SWEEP_COMBINATIONS + "."
            );
        }

        List<SweepEvaluation> evaluations = sweepConfigs.stream()
                .map(config -> {
                    SimulationResult result = simulateWithCandles(
                            symbol,
                            config,
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
                    return new SweepEvaluation(config, result, objectiveScore(optimizeFor, result));
                })
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .toList();

        int resultCount = Math.min(maxResults, evaluations.size());
        List<ParameterSweepResponse.SweepResult> rankedResults = new java.util.ArrayList<>(resultCount);
        for (int i = 0; i < resultCount; i++) {
            rankedResults.add(toSweepResult(evaluations.get(i), i + 1));
        }
        ParameterSweepResponse.SweepResult bestResult = rankedResults.isEmpty() ? null : rankedResults.get(0);

        return new ParameterSweepResponse(
                strategyType.apiValue(),
                optimizeFor.apiValue(),
                evaluations.size(),
                rankedResults.size(),
                bestResult,
                rankedResults
        );
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
                        roundMetric(rightRun.getEndingEquity().subtract(leftRun.getEndingEquity()).doubleValue()),
                        roundMetric(rightRun.getTotalReturnPct() - leftRun.getTotalReturnPct()),
                        roundMetric(rightRun.getMaxDrawdownPct() - leftRun.getMaxDrawdownPct()),
                        roundMetric(rightRun.getSharpeRatio() - leftRun.getSharpeRatio()),
                        roundMetric(rightRun.getWinRatePct() - leftRun.getWinRatePct()),
                        roundMetric(rightRun.getProfitFactor() - leftRun.getProfitFactor()),
                        roundMetric(rightRun.getExpectancy() - leftRun.getExpectancy()),
                        roundMetric(rightRun.getAverageWin() - leftRun.getAverageWin()),
                        roundMetric(rightRun.getAverageLoss() - leftRun.getAverageLoss()),
                        roundMetric(rightRun.getExposureTimePct() - leftRun.getExposureTimePct()),
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
        SimulationResult result = simulateWithCandles(
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
        SimulationRunEntity persistedRun = persistRun(result, symbol);
        return toBacktestRunResponse(persistedRun);
    }

    private SimulationResult simulateWithCandles(
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
        return simulationEngine.run(simulationRequest, selectedStrategy);
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
                result.metrics().profitFactor(),
                result.metrics().expectancy(),
                result.metrics().averageWin(),
                result.metrics().averageLoss(),
                result.metrics().exposureTimePct(),
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
                        run.getProfitFactor(),
                        run.getExpectancy(),
                        run.getAverageWin(),
                        run.getAverageLoss(),
                        run.getExposureTimePct(),
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
                run.getProfitFactor(),
                run.getExpectancy(),
                run.getAverageWin(),
                run.getAverageLoss(),
                run.getExposureTimePct(),
                run.getTradeCount()
        );
    }

    private SimulationRunEntity loadRun(long runId) {
        return simulationRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulation run not found for id=" + runId));
    }

    private double roundMetric(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
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

    /**
     * Builds concrete strategy-config combinations from sweep ranges.
     */
    private List<StrategyConfig> buildSweepConfigs(
            StrategyType strategyType,
            SmaSweepRange smaRange,
            MeanReversionSweepRange meanReversionRange
    ) {
        return switch (strategyType) {
            case SMA_CROSS -> buildSmaConfigs(smaRange);
            case MEAN_REVERSION -> buildMeanReversionConfigs(meanReversionRange);
        };
    }

    private List<StrategyConfig> buildSmaConfigs(SmaSweepRange range) {
        List<StrategyConfig> configs = new java.util.ArrayList<>();
        for (int shortWindow = range.shortWindowStart(); shortWindow <= range.shortWindowEnd(); shortWindow += range.shortWindowStep()) {
            for (int longWindow = range.longWindowStart(); longWindow <= range.longWindowEnd(); longWindow += range.longWindowStep()) {
                if (shortWindow < longWindow) {
                    configs.add(new SmaCrossConfig(shortWindow, longWindow));
                }
            }
        }
        return configs;
    }

    private List<StrategyConfig> buildMeanReversionConfigs(MeanReversionSweepRange range) {
        List<StrategyConfig> configs = new java.util.ArrayList<>();
        for (int window = range.windowStart(); window <= range.windowEnd(); window += range.windowStep()) {
            for (BigDecimal threshold : decimalRange(range.thresholdStartPct(), range.thresholdEndPct(), range.thresholdStepPct())) {
                configs.add(new MeanReversionConfig(window, threshold));
            }
        }
        return configs;
    }

    private List<BigDecimal> decimalRange(BigDecimal start, BigDecimal end, BigDecimal step) {
        List<BigDecimal> values = new java.util.ArrayList<>();
        BigDecimal current = start;
        int guard = 0;
        while (current.compareTo(end) <= 0) {
            values.add(current);
            current = current.add(step);
            guard++;
            if (guard > 5000) {
                throw new IllegalArgumentException("Decimal sweep range generated too many values.");
            }
        }
        return values;
    }

    /**
     * Converts a simulation result into a sortable objective score.
     */
    private double objectiveScore(SweepObjective objective, SimulationResult result) {
        return switch (objective) {
            case TOTAL_RETURN_PCT -> result.metrics().totalReturnPct();
            case MAX_DRAWDOWN_PCT -> -result.metrics().maxDrawdownPct();
            case SHARPE_RATIO -> result.metrics().sharpeRatio();
            case WIN_RATE_PCT -> result.metrics().winRatePct();
            case PROFIT_FACTOR -> result.metrics().profitFactor();
            case EXPECTANCY -> result.metrics().expectancy();
        };
    }

    private ParameterSweepResponse.SweepResult toSweepResult(SweepEvaluation evaluation, int rank) {
        StrategyConfig config = evaluation.config();
        ParameterSweepResponse.SweepParameters parameters = switch (config) {
            case SmaCrossConfig sma ->
                    new ParameterSweepResponse.SweepParameters(sma.shortWindow(), sma.longWindow(), null, null);
            case MeanReversionConfig mean ->
                    new ParameterSweepResponse.SweepParameters(null, null, mean.window(), mean.thresholdPct());
        };

        return new ParameterSweepResponse.SweepResult(
                rank,
                evaluation.result().strategyName(),
                parameters,
                evaluation.result().endingEquity(),
                toMetricsDto(evaluation.result().metrics()),
                roundMetric(evaluation.score())
        );
    }

    private BacktestRunResponse.MetricsDto toMetricsDto(PerformanceMetrics metrics) {
        return new BacktestRunResponse.MetricsDto(
                metrics.totalReturnPct(),
                metrics.maxDrawdownPct(),
                metrics.sharpeRatio(),
                metrics.winRatePct(),
                metrics.profitFactor(),
                metrics.expectancy(),
                metrics.averageWin(),
                metrics.averageLoss(),
                metrics.exposureTimePct(),
                metrics.tradeCount()
        );
    }

    private record SweepEvaluation(StrategyConfig config, SimulationResult result, double score) {
    }
}
