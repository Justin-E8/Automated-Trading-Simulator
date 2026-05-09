package com.tradingsim.application;

import com.tradingsim.api.dto.BacktestRunRequest;
import com.tradingsim.api.dto.BacktestRunResponse;
import com.tradingsim.domain.Candle;
import com.tradingsim.engine.EquityPoint;
import com.tradingsim.engine.SimulationEngine;
import com.tradingsim.engine.SimulationRequest;
import com.tradingsim.engine.SimulationResult;
import com.tradingsim.strategy.MovingAverageCrossStrategy;
import com.tradingsim.strategy.TradingStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimulationService {

    private final SimulationEngine simulationEngine;

    public SimulationService(SimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
    }

    public BacktestRunResponse runBacktest(BacktestRunRequest request) {
        if (request.shortWindow() >= request.longWindow()) {
            throw new IllegalArgumentException("Expected shortWindow < longWindow.");
        }

        List<Candle> candles = request.candles().stream()
                .map(c -> new Candle(c.timestamp(), c.open(), c.high(), c.low(), c.close(), c.volume()))
                .toList();

        SimulationRequest simulationRequest = new SimulationRequest(
                request.symbol(),
                request.initialCash(),
                request.quantityPerTrade(),
                request.feeBps(),
                candles
        );

        TradingStrategy strategy = new MovingAverageCrossStrategy(request.shortWindow(), request.longWindow());
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

    private BacktestRunResponse.EquityPointDto toEquityPointDto(EquityPoint point) {
        return new BacktestRunResponse.EquityPointDto(point.timestamp(), point.equity());
    }
}
