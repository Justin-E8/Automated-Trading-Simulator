package com.tradingsim.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "simulation_runs")
public class SimulationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String strategyName;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal startingCash;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal endingEquity;

    @Column(nullable = false)
    private double totalReturnPct;

    @Column(nullable = false)
    private double maxDrawdownPct;

    @Column(nullable = false)
    private double sharpeRatio;

    @Column(nullable = false)
    private double winRatePct;

    @Column(nullable = false)
    private long tradeCount;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("timestamp ASC, id ASC")
    private List<SimulationTradeEntity> trades = new ArrayList<>();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("timestamp ASC, id ASC")
    private List<SimulationEquityPointEntity> equityPoints = new ArrayList<>();

    protected SimulationRunEntity() {
    }

    public SimulationRunEntity(
            String symbol,
            String strategyName,
            BigDecimal startingCash,
            BigDecimal endingEquity,
            double totalReturnPct,
            double maxDrawdownPct,
            double sharpeRatio,
            double winRatePct,
            long tradeCount
    ) {
        this.symbol = symbol;
        this.strategyName = strategyName;
        this.startingCash = startingCash;
        this.endingEquity = endingEquity;
        this.totalReturnPct = totalReturnPct;
        this.maxDrawdownPct = maxDrawdownPct;
        this.sharpeRatio = sharpeRatio;
        this.winRatePct = winRatePct;
        this.tradeCount = tradeCount;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void addTrade(SimulationTradeEntity trade) {
        trade.attachToRun(this);
        trades.add(trade);
    }

    public void addEquityPoint(SimulationEquityPointEntity equityPoint) {
        equityPoint.attachToRun(this);
        equityPoints.add(equityPoint);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public BigDecimal getStartingCash() {
        return startingCash;
    }

    public BigDecimal getEndingEquity() {
        return endingEquity;
    }

    public double getTotalReturnPct() {
        return totalReturnPct;
    }

    public double getMaxDrawdownPct() {
        return maxDrawdownPct;
    }

    public double getSharpeRatio() {
        return sharpeRatio;
    }

    public double getWinRatePct() {
        return winRatePct;
    }

    public long getTradeCount() {
        return tradeCount;
    }

    public List<SimulationTradeEntity> getTrades() {
        return trades;
    }

    public List<SimulationEquityPointEntity> getEquityPoints() {
        return equityPoints;
    }
}
