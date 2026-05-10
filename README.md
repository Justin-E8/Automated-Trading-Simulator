# Automated Trading Simulator

Backend-first Java project for deterministic strategy backtesting on historical OHLCV candles.

This project is portfolio-focused: it demonstrates simulation logic, API design, persistence, testing, and a lightweight but usable browser UI.

## Feature overview

- Upload historical candle CSV files and preview data quality/statistics
- Run backtests using:
  - SMA crossover strategy
  - Mean reversion strategy
- Apply realistic execution/risk controls:
  - fees, slippage
  - stop loss, take profit
  - max position size, max holding candles
- Persist and revisit simulation runs
- Compare two saved runs and inspect metric deltas
- Run parameter sweeps (grid search) for strategy tuning
- Explore results in a browser UI (metrics, trades, equity chart, run history)

## Tech stack

- Java 21
- Spring Boot (Web, Validation, Data JPA)
- Maven Wrapper (`./mvnw`)
- H2 (default local runtime)
- PostgreSQL (Docker Compose runtime)
- JUnit 5 + Spring Boot Test
- Testcontainers (PostgreSQL integration test coverage)
- HTML/CSS/Vanilla JS frontend

## Architecture

- `domain` - candle/trade primitives
- `strategy` - strategy interfaces, implementations, and typed configs
- `engine` - deterministic simulation loop and metrics
- `application` - use-case orchestration and persistence mapping
- `api` - REST controllers and DTO contracts
- `infrastructure` - CSV parsing and JPA repositories

Architecture diagram:

- `docs/architecture-diagram.md`

## Prerequisites

### Required

- Java 21
- Git

### Optional (for persistent PostgreSQL mode)

- Docker Desktop (or Docker Engine + Compose)

### Optional (for CLI Yahoo CSV helper script)

- Python 3.10+
- `yfinance`, `pandas`

Install Python deps:

```bash
pip install yfinance pandas
```

## Run modes

### Mode A: Local quick run (H2 in-memory)

Use this for fastest development loop.

```bash
./mvnw spring-boot:run
```

Open:

- `http://localhost:8080`

Persistence behavior:

- Runs are saved while app is running
- Data resets after app restart (in-memory DB)

### Mode B: Docker Compose run (PostgreSQL persistent)

Use this for durable saved runs across restarts.

```bash
docker compose up --build
```

Open:

- `http://localhost:8080`

Persistence behavior:

- Runs are stored in PostgreSQL
- Data persists across app/container/computer restarts
- Data is removed only if you delete volumes (`docker compose down -v`)

PostgreSQL is intentionally not exposed on a host port to avoid local `5432` conflicts.

## Stop / restart commands

Stop while preserving data:

```bash
docker compose down
```

Stop and delete database data:

```bash
docker compose down -v
```

Restart later:

```bash
docker compose up -d
```

## Running tests

```bash
./mvnw test
```

Notes:

- Testcontainers PostgreSQL integration tests automatically skip when Docker is unavailable
- All other tests still run normally

## Getting real-world CSV data

### Option 1: Direct Yahoo Finance export

1. Open Yahoo Finance for your ticker (example: AAPL)
2. Open **Historical Data**
3. Choose date range and interval
4. Download CSV
5. Upload that CSV directly in the app

### Option 2: Built-in helper script

Generate Yahoo-compatible CSV from terminal:

```bash
python3 scripts/fetch_yahoo_csv.py --ticker AAPL --start 2024-01-01 --end 2025-01-01 --interval 1d
```

Default output:

```text
data/generated-csv/AAPL-2024-01-01-2025-01-01.csv
```

Custom output directory:

```bash
python3 scripts/fetch_yahoo_csv.py --ticker AAPL --start 2024-01-01 --end 2025-01-01 --out-dir data/my-csvs
```

Custom output file:

```bash
python3 scripts/fetch_yahoo_csv.py --ticker AAPL --start 2024-01-01 --end 2025-01-01 --out data/custom/aapl.csv
```

## Supported CSV formats

### Simulator canonical format

```text
timestamp,open,high,low,close,volume
2025-01-01T09:30:00,100.00,101.00,99.00,100.50,1000
```

### Yahoo export format (accepted directly)

```text
Date,Open,High,Low,Close,Adj Close,Volume
```

Validation enforced:

- strictly increasing timestamps
- positive OHLC prices
- non-negative volume
- OHLC consistency checks

## UI workflow

1. Start app (`./mvnw spring-boot:run` or `docker compose up --build`)
2. Open `http://localhost:8080`
3. Choose strategy + risk settings
4. Upload CSV and click **Preview CSV**
5. Click **Run Backtest from CSV**
6. Review:
   - metric cards
   - equity curve
   - trades table
7. Use **Saved Runs**:
   - load run history
   - filter by symbol/strategy
   - load old runs into the results pane
8. Use **Run Comparison**:
   - set left/right IDs
   - compare delta metrics
9. (Optional) Expand **Advanced: Parameter Sweep** and run grid search

Tip: info buttons (`i`) in controls and metric cards explain each setting/metric in plain language.

## API reference (core endpoints)

- `POST /api/v1/simulations/csv/preview`
- `POST /api/v1/simulations/csv/backtest`
- `POST /api/v1/simulations/csv/sweep`
- `GET /api/v1/simulations/runs/{runId}`
- `GET /api/v1/simulations/runs`
- `GET /api/v1/simulations/runs/compare?leftRunId={id}&rightRunId={id}`

### Quick cURL examples

List saved runs:

```bash
curl "http://localhost:8080/api/v1/simulations/runs?page=0&size=10"
```

Fetch one run:

```bash
curl "http://localhost:8080/api/v1/simulations/runs/1"
```

Compare two runs:

```bash
curl "http://localhost:8080/api/v1/simulations/runs/compare?leftRunId=1&rightRunId=2"
```

### Standard error payload

```json
{
  "timestamp": "2026-05-10T21:20:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "must be greater than 0",
  "error": "must be greater than 0",
  "path": "/api/v1/simulations/runs/compare",
  "validationErrors": [
    { "field": "leftRunId", "message": "must be greater than 0" }
  ]
}
```

`error` is retained as a compatibility alias for frontend parsing.

## Portfolio notes

This repository demonstrates:

- deterministic simulation engine design
- typed strategy configuration and factory-based wiring
- API contract quality (validation + consistent errors)
- persistence modeling with run/trade/equity entities
- integration/performance testing discipline
- Dockerized local deployment path

## Additional docs

- `docs/project-plan.md` - implementation roadmap and architecture evolution
- `docs/architecture-diagram.md` - system component and data-flow diagram