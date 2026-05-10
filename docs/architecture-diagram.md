# Architecture Diagram

This diagram shows how data moves through the simulator from UI/API input to strategy execution, persistence, and response rendering.

```mermaid
flowchart TD
    UI[Browser UI<br/>index.html + app.js] -->|HTTP JSON / multipart| API[SimulationController<br/>api layer]
    API --> APP[SimulationService<br/>application layer]

    APP --> CSV[CsvCandleService<br/>infrastructure/csv]
    CSV --> VALID[CandleValidationService]
    CSV --> APP

    APP --> FACTORY[StrategyFactory]
    FACTORY --> SMA[MovingAverageCrossStrategy]
    FACTORY --> MR[MeanReversionStrategy]

    APP --> ENGINE[SimulationEngine<br/>engine layer]
    ENGINE --> DOMAIN[Domain Models<br/>Candle / Trade / EquityPoint]
    ENGINE --> METRICS[PerformanceMetrics]

    APP --> REPO[SimulationRunRepository<br/>Spring Data JPA]
    REPO --> DB[(H2 or PostgreSQL)]

    APP --> DTO[Response DTOs<br/>Backtest / History / Compare / Sweep]
    DTO --> API
    API --> UI

    ERR[ApiExceptionHandler] --> API
```

## Layer responsibilities

- **api**: request validation, parameter mapping, response contracts
- **application**: orchestrates use-cases and persistence
- **strategy**: pure strategy signals based on candle history
- **engine**: deterministic execution loop + risk exits + metrics
- **infrastructure**: CSV parsing and database access
- **domain**: immutable records representing market and trade data
