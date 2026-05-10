package com.tradingsim.infrastructure.persistence;

import com.tradingsim.domain.OrderSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Persisted trade row belonging to a simulation run.
 */
@Entity
@Table(name = "simulation_trades")
public class SimulationTradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private SimulationRunEntity run;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal fee;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal realizedPnl;

    protected SimulationTradeEntity() {
    }

    public SimulationTradeEntity(
            LocalDateTime timestamp,
            String symbol,
            OrderSide side,
            long quantity,
            BigDecimal price,
            BigDecimal fee,
            BigDecimal realizedPnl
    ) {
        this.timestamp = timestamp;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.fee = fee;
        this.realizedPnl = realizedPnl;
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

    public String getSymbol() {
        return symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public long getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }
}
