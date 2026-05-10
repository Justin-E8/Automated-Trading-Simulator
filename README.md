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

- Run SMA crossover backtests from uploaded CSV files
- Run mean-reversion backtests from uploaded CSV files
- Accept raw Yahoo Finance CSV exports directly (no conversion script required)
- Simulate buy/sell execution with configurable fees, slippage, and risk controls
- Track:
  - trade events
  - equity curve
  - summary metrics (return, drawdown, Sharpe, win rate, profit factor, expectancy, average win/loss, exposure time)
- Preview uploaded candle datasets (row count and date/price range)

## API endpoints (initial)

- `POST /api/v1/simulations/csv/preview`
- `POST /api/v1/simulations/csv/backtest`
- `POST /api/v1/simulations/csv/sweep`
- `GET /api/v1/simulations/runs/{runId}`
- `GET /api/v1/simulations/runs` (paged + filterable)
- `GET /api/v1/simulations/runs/compare?leftRunId={id}&rightRunId={id}`

> Note: current default persistence uses in-memory H2 (`jdbc:h2:mem`), so saved runs are available while the app is running and reset on restart.

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

### 3) Quick API check (CSV preview)

With the app running, verify CSV preview endpoint:

```bash
printf '%s\n' \
'timestamp,open,high,low,close,volume' \
'2025-01-01T09:30:00,100,101,99,100.5,1000' \
'2025-01-02T09:30:00,100.5,102,100,101.7,1200' \
| curl -X POST http://localhost:8080/api/v1/simulations/csv/preview -F "file=@-;filename=quick-test.csv;type=text/csv"
```

### 4) Use the built-in simulator UI

Once the app is running, open:

- `http://localhost:8080`

From there you can:

- choose strategy (SMA Cross or Mean Reversion)
- upload a CSV, preview candle stats, and run a CSV-based backtest
- inspect metrics, trades, and equity curve output

### CSV backtest parameters

`POST /api/v1/simulations/csv/backtest` accepts multipart form fields:

- `file` (CSV upload)
- `symbol`
- `strategy`: `sma-cross` (default) or `mean-reversion`
- `initialCash`
- `quantityPerTrade`
- `feeBps`
- `slippageBps` (`0` disables slippage)
- `maxPositionSize` (`0` means use quantityPerTrade limit)
- `maxHoldingCandles` (`0` disables holding-period exits)
- `stopLossPct` (`0` disables stop-loss exits)
- `takeProfitPct` (`0` disables take-profit exits)
- SMA params:
  - `shortWindow`
  - `longWindow`
- Mean reversion params:
  - `meanReversionWindow`
  - `meanReversionThresholdPct`

### Parameter sweep endpoint

`POST /api/v1/simulations/csv/sweep` runs grid-search combinations and ranks them by an objective.

Core fields:
- all standard backtest controls (`file`, `symbol`, strategy/risk/execution controls)
- `optimizeFor`:
  - `total-return-pct`
  - `max-drawdown-pct` (lower drawdown is better)
  - `sharpe-ratio`
  - `win-rate-pct`
  - `profit-factor`
  - `expectancy`
- `maxResults` (top N combinations returned)

SMA sweep range fields:
- `shortWindowStart`, `shortWindowEnd`, `shortWindowStep`
- `longWindowStart`, `longWindowEnd`, `longWindowStep`

Mean reversion sweep range fields:
- `meanReversionWindowStart`, `meanReversionWindowEnd`, `meanReversionWindowStep`
- `meanReversionThresholdStartPct`, `meanReversionThresholdEndPct`, `meanReversionThresholdStepPct`

Backtest responses include `runId`, which can be used to fetch the saved run later:

```bash
curl http://localhost:8080/api/v1/simulations/runs/1
```

List run history:

```bash
curl "http://localhost:8080/api/v1/simulations/runs?page=0&size=10&symbol=MSFT&strategy=mean"
```

Compare two runs (delta fields are `right - left`):

```bash
curl "http://localhost:8080/api/v1/simulations/runs/compare?leftRunId=1&rightRunId=2"
```

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

Use a custom output directory:

```bash
python3 scripts/fetch_yahoo_csv.py --ticker AAPL --start 2024-01-01 --end 2025-01-01 --out-dir data/my-csvs
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

## Planned next steps

1. Quality and reliability hardening (expand deterministic tests and integration coverage)
2. Standardize API error schema and edge-case handling
3. Add performance tests for larger datasets and sweep workloads
4. Improve saved-run and comparison UX in the lightweight UI
5. Add durable DB profile (e.g., PostgreSQL) when ready for persistent multi-session history

---

See `docs/project-plan.md` for deeper scope and architecture details.
For branch promotion steps, see `docs/merge-checklist.md`.