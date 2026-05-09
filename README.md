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
- Run backtests from uploaded CSV files (no candle JSON editing required)
- Accept raw Yahoo Finance CSV exports directly (no conversion script required)
- Fetch real market candles by symbol/date range and run backtests without CSV upload
- Simulate buy/sell execution with configurable fee basis points
- Track:
  - trade events
  - equity curve
  - summary metrics (return, drawdown, Sharpe, win rate)
- Preview uploaded candle datasets (row count and date/price range)

## API endpoints (initial)

- `POST /api/v1/simulations/backtest`
- `POST /api/v1/simulations/csv/preview`
- `POST /api/v1/simulations/csv/backtest`
- `POST /api/v1/simulations/market-data/fetch`
- `POST /api/v1/simulations/market-data/backtest`
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

No global Maven install is required; use the included Maven Wrapper.

### VS Code quick start

1. Install VS Code extensions from recommended list (`.vscode/extensions.json`):
   - Extension Pack for Java
   - Maven for Java
   - Spring Boot Extension Pack
2. Open this repository folder in VS Code.
3. Wait for Java project import/indexing to finish.
4. Use one of these options to run:
   - Run task: `Terminal` -> `Run Task` -> `mvnw: spring-boot:run`
   - Debug launch: `Run and Debug` -> `Debug TradingSimulatorApplication`

### 1) Start the API

```bash
./mvnw spring-boot:run
```

### 2) Run tests

```bash
./mvnw test
```

### 3) Quick API check

With the app running, verify sample data endpoint:

```bash
curl http://localhost:8080/api/v1/simulations/sample-candles
```

### 4) Use the built-in simulator UI

Once the app is running, open:

- `http://localhost:8080`

From there you can:

- load sample candles
- adjust strategy parameters
- run a JSON-based backtest
- upload a CSV, preview candle stats, and run a CSV-based backtest
- fetch market candles by symbol/date range and run market-data backtests
- inspect metrics, trades, equity curve, and raw JSON output

### Recommended real-world stock workflow (no API key)

1. Download historical CSV from Yahoo Finance (e.g., AAPL historical data page).
2. In app UI, use **CSV Upload Mode (Recommended)**.
3. Upload the raw Yahoo CSV directly.
4. Click **Preview CSV** then **Run Backtest from CSV**.

No conversion script is needed.

### Quick CLI helper to generate Yahoo CSV

If you prefer terminal-based download instead of web UI export, use:

```bash
python3 scripts/fetch_yahoo_csv.py --ticker AAPL --start 2024-01-01 --end 2025-01-01 --interval 1d
```

By default this saves into:

```text
data/generated-csv/AAPL-2024-01-01-2025-01-01.csv
```

You can override destination if needed:

```bash
python3 scripts/fetch_yahoo_csv.py --ticker AAPL --start 2024-01-01 --end 2025-01-01 --out data/custom/aapl.csv
```

Install required Python packages once:

```bash
pip install yfinance pandas
```

### CSV format for upload

Required header columns:

```text
timestamp,open,high,low,close,volume
```

Also accepted directly (Yahoo Finance export):

```text
Date,Open,High,Low,Close,Adj Close,Volume
```

Validation enforced by backend:

- timestamps must be strictly increasing
- open/high/low/close must be positive decimals
- volume must be non-negative
- OHLC consistency checks are enforced (high/low vs open/close)

Example row:

```text
2025-01-01T09:30:00,100.00,101.00,99.00,100.50,1000
```

### Market data mode notes

- Current provider adapter: **Alpha Vantage** (daily interval `1d`)
- Uses Alpha Vantage `TIME_SERIES_DAILY` CSV endpoint with `outputsize=compact` (standard/free API keys)
- Compact mode generally returns recent ~100 trading days
- Because third-party APIs can have limits, CSV upload is the most reliable default workflow.
- Configure API key with env var:
  - `export ALPHA_VANTAGE_API_KEY=your_key_here`
- Enter symbol + start/end date in UI and click:
  - `Fetch Market Data` (preview stats/sample candles)
  - `Run Backtest from Market Data`
- Provider responses are normalized and validated with the same candle rules as CSV ingestion.
- Fetched datasets are cached in-memory for repeated same-range requests.

## Planned next steps

1. Multi-strategy framework (add mean reversion strategy and strategy selector)
2. Persist simulation runs/results in PostgreSQL
3. Add risk controls (position sizing, stop loss, max drawdown guardrails)
4. Add richer analysis metrics and parameter sweeps
5. Add additional market data providers beyond initial adapter
6. Keep improving the lightweight UI before considering a heavier frontend stack

---

See `docs/project-plan.md` for deeper scope and architecture details.