# Automated Trading Simulator

Backend-first Java project for simulating algorithmic trading strategies on historical market data.

## Why this project exists

This repository is designed to become a resume-quality software engineering project:

- deterministic backtesting engine
- API-first architecture
- clean separation between strategy logic and infrastructure
- upgrade path to a richer frontend later

## Current stack

- Java 21
- Spring Boot (REST API + validation + app lifecycle)
- Maven
- H2 (dev runtime DB)
- JUnit 5 / Spring Boot Test

## Current backend capabilities

- Run SMA crossover backtests through a REST endpoint
- Simulate buy/sell execution with configurable fee basis points
- Track:
  - trade events
  - equity curve
  - summary metrics (return, drawdown, Sharpe, win rate)
- Example sample-candle endpoint for fast testing

## API endpoints (initial)

- `POST /api/v1/simulations/backtest`
- `GET /api/v1/simulations/sample-candles`

### Example request body (`POST /api/v1/simulations/backtest`)

```json
{
  "symbol": "AAPL",
  "initialCash": 10000.0,
  "quantityPerTrade": 10,
  "feeBps": 5.0,
  "shortWindow": 3,
  "longWindow": 5,
  "candles": [
    {"timestamp":"2025-01-01T09:30:00","open":100.0,"high":100.0,"low":100.0,"close":100.0,"volume":1000},
    {"timestamp":"2025-01-02T09:30:00","open":101.0,"high":101.0,"low":101.0,"close":101.0,"volume":1000},
    {"timestamp":"2025-01-03T09:30:00","open":102.0,"high":102.0,"low":102.0,"close":102.0,"volume":1000},
    {"timestamp":"2025-01-04T09:30:00","open":103.5,"high":103.5,"low":103.5,"close":103.5,"volume":1000},
    {"timestamp":"2025-01-05T09:30:00","open":104.2,"high":104.2,"low":104.2,"close":104.2,"volume":1000},
    {"timestamp":"2025-01-06T09:30:00","open":104.0,"high":104.0,"low":104.0,"close":104.0,"volume":1000}
  ]
}
```

## Local setup

### 1) Start the API

```bash
mvn spring-boot:run
```

### 2) Run tests

```bash
mvn test
```

## Planned next steps

1. CSV ingestion pipeline for historical candles
2. Persist simulation runs/results in PostgreSQL
3. Add risk controls (position sizing, stop loss, max drawdown guardrails)
4. Add extra strategies (mean reversion, breakout)
5. Add frontend dashboard (React + charting) once backend contracts stabilize

---

See `docs/project-plan.md` for deeper scope and architecture details.