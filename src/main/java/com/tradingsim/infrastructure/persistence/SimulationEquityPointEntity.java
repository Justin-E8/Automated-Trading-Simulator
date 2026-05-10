package com.tradingsim.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persisted equity-curve point belonging to a simulation run.
 */
@Entity
@Table(name = "simulation_equity_points")
public class SimulationEquityPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private SimulationRunEntity run;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal equity;

    protected SimulationEquityPointEntity() {
    }

    public SimulationEquityPointEntity(LocalDateTime timestamp, BigDecimal equity) {
        this.timestamp = timestamp;
        this.equity = equity;
    }

    void attachToRun(SimulationRunEntity run) {
        this.run = run;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public BigDecimal getEquity() {
        return equity;
    }
}
