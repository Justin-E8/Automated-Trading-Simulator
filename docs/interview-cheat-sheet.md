# Automated Trading Simulator - Interview Cheat Sheet

Use this as a deep technical prep guide for internship interviews.  
Every section includes direct references to the exact code that implements the behavior.

---

## 1) 30-second project pitch

I built a backend-first automated trading simulator in Java 21 + Spring Boot that runs deterministic backtests on historical CSV candles, supports multiple strategies (SMA crossover and mean reversion), realistic risk controls (fees, slippage, stop loss, take profit, max holding, max position), persistence (H2 + PostgreSQL), run-history comparison, parameter sweep optimization, and a lightweight web UI for end-to-end usage.

**Code anchors**
- App entry point: `src/main/java/com/tradingsim/TradingSimulatorApplication.java`
- Main API surface: `src/main/java/com/tradingsim/api/SimulationController.java`
- Core orchestrator: `src/main/java/com/tradingsim/application/SimulationService.java`
- Simulation engine: `src/main/java/com/tradingsim/engine/SimulationEngine.java`

---

## 2) Architecture overview

For the visual component/data-flow diagram, see:
- `docs/architecture-diagram.md`

### Layered architecture (what lives where)

| Layer | Responsibility | Key code |
|---|---|---|
| `domain` | Immutable core market/trade records | `domain/Candle.java`, `domain/Trade.java`, `domain/OrderSide.java` |
| `strategy` | Signal-generation abstractions + implementations | `strategy/TradingStrategy.java`, `MovingAverageCrossStrategy.java`, `MeanReversionStrategy.java`, `StrategyFactory.java` |
| `engine` | Deterministic execution loop, risk exits, metrics | `engine/SimulationEngine.java`, `SimulationRequest.java`, `SimulationResult.java`, `PerformanceMetrics.java` |
| `application` | Use-case orchestration, strategy wiring, persistence mapping | `application/SimulationService.java`, `CandleValidationService.java` |
| `api` | REST endpoints, request validation, error contracts | `api/SimulationController.java`, `api/ApiExceptionHandler.java`, `api/dto/*` |
| `infrastructure` | CSV parser + JPA persistence implementations | `infrastructure/csv/CsvCandleService.java`, `infrastructure/persistence/*` |
| `frontend` | Browser controls and result rendering | `src/main/resources/static/index.html`, `app.js`, `styles.css` |

### Why this is interview-strong

- Clean separation of concerns (controller is thin; engine logic isolated)
- Domain logic does not depend on Spring annotations
- Strategy implementations are swappable via typed config + factory
- End-to-end workflows have automated tests (unit + integration + performance baseline)

---

## 3) End-to-end request flows (what happens for each API)

## A) CSV preview flow (`POST /api/v1/simulations/csv/preview`)

1. Controller receives multipart CSV file  
   - `SimulationController.previewCsv(...)`
2. Calls application service preview use-case  
   - `SimulationService.previewCsv(...)`
3. Service calls CSV parser/normalizer  
   - `CsvCandleService.preview(...)` -> internally `parseCandles(...)`
4. Parser:
   - resolves column aliases (`Date`/`Adj Close` support)
   - parses timestamps/decimals/volume
   - validates each candle + strict chronological order
5. Returns summary DTO with sample candles  
   - `CsvPreviewSummary` -> `CsvPreviewResponse`

**Code anchors**
- `api/SimulationController.java` (`previewCsv`)
- `application/SimulationService.java` (`previewCsv`)
- `infrastructure/csv/CsvCandleService.java` (`parseCandles`, `preview`, alias map)
- `application/CandleValidationService.java`

## B) Backtest flow (`POST /api/v1/simulations/csv/backtest`)

1. Controller validates request params (`@Min`, `@DecimalMin`, etc.)
2. Converts strategy form params into typed config:
   - `SmaCrossConfig` or `MeanReversionConfig`
3. Service parses CSV and builds simulation request
4. Service uses `StrategyFactory` to instantiate strategy
5. `SimulationEngine.run(...)` executes candle-by-candle loop:
   - gets strategy signal per candle
   - applies slippage and fee models
   - enforces risk exits (stop loss / take profit / max holding)
   - updates cash, position, trades, equity curve
6. Service persists run + nested trades/equity points via repository
7. Service maps persisted entity to API response DTO with `runId`

**Code anchors**
- `api/SimulationController.java` (`runBacktestFromCsv`, `toStrategyConfig`)
- `application/SimulationService.java` (`runBacktestFromCsv`, `simulateWithCandles`, `persistRun`)
- `strategy/StrategyFactory.java`
- `engine/SimulationEngine.java` (`run`, `applySlippage`, `shouldExitForRisk`, `calculateMetrics`)
- `infrastructure/persistence/SimulationRunRepository.java`
- `api/dto/BacktestRunResponse.java`

## C) Parameter sweep flow (`POST /api/v1/simulations/csv/sweep`)

1. Controller validates sweep ranges and objective
2. Service builds all strategy combinations from range objects
3. Service guards against runaway combinations:
   - `MAX_SWEEP_COMBINATIONS = 2000`
4. For each config:
   - run simulation (without persisting runs)
   - compute objective score
5. Sort descending by objective score
6. Return top `maxResults` ranked output

**Code anchors**
- `api/SimulationController.java` (`runParameterSweepFromCsv`)
- `application/SimulationService.java` (`runParameterSweepFromCsv`, `buildSweepConfigs`, `objectiveScore`)
- `strategy/SmaSweepRange.java`, `strategy/MeanReversionSweepRange.java`
- `api/dto/ParameterSweepResponse.java`, `api/dto/SweepObjective.java`

## D) Run history flow (`GET /api/v1/simulations/runs`)

1. Controller validates page/size params
2. Service enforces bounds (`size` between 1 and 100)
3. Repository performs case-insensitive symbol/strategy filtering + paging
4. Service maps entities to summary DTOs

**Code anchors**
- `api/SimulationController.java` (`listRuns`)
- `application/SimulationService.java` (`listRuns`)
- `infrastructure/persistence/SimulationRunRepository.java`
- `api/dto/RunHistoryResponse.java`, `RunSummaryResponse.java`

## E) Run comparison flow (`GET /api/v1/simulations/runs/compare`)

1. Controller validates run IDs are positive
2. Service enforces IDs must be different
3. Service loads both runs from repository
4. Computes delta metrics as **right - left**

**Code anchors**
- `api/SimulationController.java` (`compareRuns`)
- `application/SimulationService.java` (`compareRuns`, `loadRun`)
- `api/dto/RunComparisonResponse.java`

---

## 4) Technology deep dive

## Spring Boot (how it is used)

### Bootstrapping
- `@SpringBootApplication` starts component scanning and auto-configuration  
  - `TradingSimulatorApplication.java`

### Dependency injection + managed beans
- `@RestController`: `SimulationController`
- `@Service`: `SimulationService`, `CsvCandleService`, `CandleValidationService`
- `@Component`: `SimulationEngine`, `StrategyFactory`

Constructor injection is used throughout for explicit dependencies.

### REST + validation
- Endpoint mapping via `@PostMapping`, `@GetMapping`, `@RequestParam`, `@PathVariable`
- Request validation via Jakarta annotations:
  - `@Min`, `@Max`, `@DecimalMin`, `@Positive`, `@NotBlank`
- Class-level `@Validated` on controller activates parameter constraint checks

### Global exception handling
- `@RestControllerAdvice` in `ApiExceptionHandler`
- Standardized payload (`ApiErrorResponse`):
  - `timestamp`, `status`, `code`, `message`, `error`, `path`, `validationErrors`
- Dedicated handling for:
  - business bad requests
  - bean/constraint validation
  - not found (`ResourceNotFoundException`)
  - fallback internal errors

**Code anchors**
- `api/ApiExceptionHandler.java`
- `api/dto/ApiErrorResponse.java`
- `application/ResourceNotFoundException.java`

## Spring Data JPA + persistence

### Entity model
- `SimulationRunEntity` = aggregate root (one run)
- `SimulationTradeEntity` + `SimulationEquityPointEntity` child rows
- One-to-many with cascade + orphan removal from run to children

### Repository abstraction
- `SimulationRunRepository extends JpaRepository<SimulationRunEntity, Long>`
- Custom paging/filter method:
  - `findAllBySymbolContainingIgnoreCaseAndStrategyNameContainingIgnoreCase(...)`

### DB modes
- Default app config points to in-memory H2
  - `application.yml` datasource URL
- Docker compose injects PostgreSQL datasource environment variables
  - `docker-compose.yml`

**Code anchors**
- `infrastructure/persistence/*.java`
- `src/main/resources/application.yml`
- `docker-compose.yml`

## Maven and build tooling

### Why Maven wrapper (`./mvnw`)
- Ensures every machine uses consistent Maven distribution
- Wrapper distribution configured in:
  - `.mvn/wrapper/maven-wrapper.properties`

### Project dependencies
- Spring Web
- Validation
- Data JPA
- H2 runtime
- PostgreSQL runtime
- Spring Boot Test
- Testcontainers (`junit-jupiter`, `postgresql`)

### Build plugin
- `spring-boot-maven-plugin` packages/runs app

**Code anchors**
- `pom.xml`
- `.mvn/wrapper/maven-wrapper.properties`

## Docker and runtime packaging

### `Dockerfile`
- Multi-stage build:
  1. Maven image builds jar with wrapper
  2. JRE image runs packaged jar
- Uses `MAVEN_CONFIG=""` in build stage to avoid wrapper arg issue in container env

### `docker-compose.yml`
- `postgres` service with healthcheck and named volume
- `app` service depends on healthy DB and injects Spring datasource env vars
- exposes app at host `8080:8080`

**Code anchors**
- `Dockerfile`
- `docker-compose.yml`

---

## 5) Core engine logic you should be ready to explain

## Determinism

`SimulationEngine.run(...)` iterates candles sequentially.  
No randomness, no async execution, no parallel mutation.  
Given same candles and parameters, output is deterministic.

## Position/risk model

- Single-position model (`openQuantity` long-only state)
- Entry:
  - signal `BUY`, no open position, enough cash
- Exit:
  - signal `SELL`
  - or risk exits triggered:
    - `maxHoldingCandles`
    - `stopLossPct`
    - `takeProfitPct`
- Fees and slippage applied on both entry and exit

## Metric calculations (high-level)

- Total Return % = `(endingEquity - initialCash) / initialCash * 100`
- Max Drawdown % = max peak-to-trough equity decline
- Sharpe = mean(return series) / std(return series) * sqrt(252)
- Win Rate % = winning closed trades / closed trades
- Profit Factor = gross profit / absolute gross loss
- Expectancy = average realized PnL per closed trade
- Average Win / Loss from realized sell trade outcomes
- Exposure Time % = bars with open position / total bars

**Code anchors**
- `engine/SimulationEngine.java`

---

## 6) Strategy system design

## Abstractions

- `TradingStrategy` interface:
  - `name()`
  - `generateSignal(history, openQuantity)`
- `StrategySignal` enum: `BUY`, `SELL`, `HOLD`
- Typed strategy config via sealed interface:
  - `StrategyConfig` permits `SmaCrossConfig`, `MeanReversionConfig`

## Factory wiring

`StrategyFactory.create(config)` maps config type -> executable strategy implementation.

This design prevents stringly-typed strategy logic from leaking into engine internals and improves compile-time safety.

## Implemented strategies

### SMA crossover
- Compute short and long SMA over trailing windows
- `shortSma > longSma` and flat => BUY
- `shortSma < longSma` and in position => SELL

### Mean reversion
- Compute moving average over lookback window
- Build upper/lower bands using threshold %
- price <= lower band and flat => BUY
- price >= upper band and in position => SELL

**Code anchors**
- `strategy/TradingStrategy.java`
- `strategy/StrategySignal.java`
- `strategy/StrategyConfig.java`
- `strategy/StrategyFactory.java`
- `strategy/MovingAverageCrossStrategy.java`
- `strategy/MeanReversionStrategy.java`

---

## 7) API contract details (what to say if asked)

## Endpoints

- `POST /api/v1/simulations/csv/preview`
- `POST /api/v1/simulations/csv/backtest`
- `POST /api/v1/simulations/csv/sweep`
- `GET /api/v1/simulations/runs/{runId}`
- `GET /api/v1/simulations/runs`
- `GET /api/v1/simulations/runs/compare`

## Input validation examples

- `initialCash >= 100.00`
- `quantityPerTrade >= 1`
- windows and thresholds have lower bounds
- pagination bounds on history
- compare IDs must be positive and different

## Error contract behavior

All errors return structured JSON payload (`ApiErrorResponse`) with HTTP-appropriate status and machine-friendly `code`.

You can explain that this improves frontend consistency and client debuggability.

**Code anchors**
- `api/SimulationController.java`
- `api/ApiExceptionHandler.java`
- `api/dto/ApiErrorResponse.java`

---

## 8) Frontend architecture (lightweight by design)

The frontend intentionally uses vanilla JS for portability and easy reviewer comprehension.

## Main responsibilities in `app.js`

- build form payloads for backtest/sweep
- call API endpoints (preview/backtest/sweep/history/compare)
- parse and render responses:
  - metrics
  - trades table
  - canvas equity curve
  - sweep ranking table
  - run history + comparison summary
- show top status banner for success/error/info
- toggle context info popovers

**Code anchors**
- `src/main/resources/static/index.html`
- `src/main/resources/static/app.js`
- `src/main/resources/static/styles.css`

---

## 9) Testing strategy (explain this clearly in interviews)

## Unit tests

- Engine rule behavior and risk exits:
  - `engine/SimulationEngineTest.java`
- Engine performance baseline:
  - `engine/SimulationEnginePerformanceTest.java`
- Strategy behavior/validation:
  - `strategy/MeanReversionStrategyTest.java`
  - `strategy/StrategyTypeTest.java`
  - `strategy/StrategyFactoryTest.java`
  - `strategy/StrategyConfigValidationTest.java`
- CSV parser normalization/validation:
  - `infrastructure/csv/CsvCandleServiceTest.java`

## Integration tests

- Service-level history/sweep workflows:
  - `application/SimulationServiceHistoryTest.java`
  - `application/SimulationServiceSweepTest.java`
- Repository persistence behavior:
  - `infrastructure/persistence/SimulationRunRepositoryTest.java`
- API error schema verification:
  - `api/ApiExceptionHandlerTest.java`
- PostgreSQL containerized API + persistence path:
  - `api/SimulationApiPostgresIntegrationTest.java`

## Key testing message

The suite validates correctness at multiple levels:
- pure logic
- API contracts
- persistence mapping
- runtime behavior against real Postgres when Docker exists

---

## 10) Interview-ready design trade-offs to discuss

## Why CSV instead of direct market API in MVP?

CSV ingestion is deterministic, reproducible, and avoids runtime key/rate-limit dependencies for interview demos.

## Why lightweight frontend?

Keeps focus on backend architecture/logic while still allowing end-to-end usability and demonstrations.

## Why both H2 and PostgreSQL?

- H2: instant local startup and quick iteration
- PostgreSQL: realistic persistence behavior and deployment parity

## Why typed strategy configs + factory?

Avoids fragile string parsing and makes strategy extension predictable and testable.

## Why global API error contract?

Simplifies client handling and gives consistent operational/debug signals.

---

## 11) Likely interview questions and concise answer cues

## Q: "How does one backtest request move through your system?"

Answer using this chain:

`SimulationController` -> `SimulationService` -> `CsvCandleService` + `StrategyFactory` -> `SimulationEngine` -> `SimulationRunRepository` -> response DTO mapping.

## Q: "How do you ensure simulation correctness?"

- deterministic sequential loop
- strong input validation
- explicit risk rule checks
- unit tests for engine and strategies
- integration tests for persistence/API behavior

## Q: "How do you handle bad input?"

- declarative controller validation annotations
- parser-level semantic checks (CSV structure + OHLC consistency + ordering)
- centralized exception handling with standard payload

## Q: "How is this deployable?"

- local mode via Maven wrapper
- persistent mode via Docker compose with PostgreSQL
- environment-driven datasource switching

---

## 12) Quick reference map (where to open first during interview prep)

- Entry point: `src/main/java/com/tradingsim/TradingSimulatorApplication.java`
- API endpoints: `src/main/java/com/tradingsim/api/SimulationController.java`
- Error model: `src/main/java/com/tradingsim/api/ApiExceptionHandler.java`
- Orchestration: `src/main/java/com/tradingsim/application/SimulationService.java`
- Engine core: `src/main/java/com/tradingsim/engine/SimulationEngine.java`
- Strategy system: `src/main/java/com/tradingsim/strategy/*`
- CSV ingest: `src/main/java/com/tradingsim/infrastructure/csv/CsvCandleService.java`
- Persistence model: `src/main/java/com/tradingsim/infrastructure/persistence/*`
- Frontend behavior: `src/main/resources/static/app.js`
- Build/runtime config: `pom.xml`, `application.yml`, `Dockerfile`, `docker-compose.yml`
- Tests: `src/test/java/com/tradingsim/**`

