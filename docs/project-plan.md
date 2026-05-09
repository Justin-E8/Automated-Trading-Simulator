# Automated Trading Simulator - Project Plan

## 1) Project Goal

Build a backend-first automated trading simulator in Java that demonstrates:

- clean backend architecture
- data processing and simulation logic
- API design and testing
- extensibility for future frontend and strategy plugins

This should be strong enough to discuss in internship interviews as a real software engineering project, not just a coding exercise.

## 2) What the project should do (MVP)

The MVP focuses on paper/backtesting simulation only (no real brokerage integration).

### Functional requirements

1. Load historical OHLCV market data from CSV.
2. Run a strategy against historical candles in time order.
3. Simulate order execution with fees and slippage assumptions.
4. Track positions, cash, equity curve, and trade history.
5. Produce summary metrics:
   - total return
   - max drawdown
   - Sharpe ratio (simple version)
   - win rate
   - number of trades
6. Expose simulation through a REST API.
7. Store simulation runs and results in a database.

### Non-functional requirements

- deterministic simulation results
- testable core logic (unit tests for engine + strategies)
- clean separation between domain logic and infrastructure
- ability to attach a frontend later without refactoring the core engine

## 3) Recommended tech stack

### Backend (chosen now)

- **Java 21** - modern LTS language features, strong ecosystem
- **Spring Boot** - fast API and production tooling
- **Maven** - standard dependency/build tool
- **PostgreSQL** (planned) - durable storage for simulation runs
- **H2** (dev profile) - fast local setup for development
- **JUnit 5 + Mockito** - testing
- **Testcontainers** (future step) - integration tests with real PostgreSQL

### Frontend (deferred/light now)

- Start with no heavy frontend framework.
- Use API-first backend + Swagger/OpenAPI docs.
- Later options:
  - React + TypeScript dashboard
  - charts for equity curve / PnL / drawdown

This keeps your first iterations backend-heavy, while preserving a clean path to a richer frontend.

## 4) Architecture direction

Use a layered + hexagonal style:

- `domain`: candles, orders, trades, portfolio, metrics
- `engine`: simulation loop + execution modeling
- `strategy`: strategy interfaces and implementations
- `application`: orchestration and use cases
- `infrastructure`: CSV loader, persistence, REST controllers

Important rule: strategy and simulation core should not depend on web/database frameworks.

## 5) Implementation sequence (exact order to finish project)

This order is intentional. Each phase unlocks the next one and avoids large refactors.

### Phase 0 - Foundation (completed)

- Spring Boot backend scaffold
- core package boundaries (`domain`, `engine`, `strategy`, `application`, `api`)
- initial SMA crossover backtest flow
- lightweight UI for running and visualizing one backtest

**Exit criteria**

- app runs locally via `./mvnw spring-boot:run`
- tests pass via `./mvnw test`

### Phase 1 - Data ingestion and normalization

1. Add CSV upload/parse support for OHLCV candles.
2. Add data validation rules (timestamp ordering, missing values, invalid prices).
3. Add API endpoint for data preview/statistics (count, min/max date).

**Exit criteria**

- user can upload CSV and run simulation without editing JSON manually
- invalid CSV is rejected with clear error responses

### Phase 2 - Multi-strategy framework

1. Add strategy selection to request model (not hardcoded to SMA crossover).
2. Implement strategy registry/factory with typed parameter objects.
3. Add at least one new strategy:
   - Mean Reversion (price deviation from moving average)
4. Update UI to choose strategy and strategy-specific parameters.

**Exit criteria**

- same candle dataset can be run with different strategies
- strategy behavior is isolated and unit-tested per strategy

### Phase 3 - Execution and risk realism

1. Add configurable slippage model.
2. Add position/risk controls:
   - max position size
   - stop loss
   - take profit
3. Add optional max holding period rule.

**Exit criteria**

- simulation output includes risk-triggered exits
- users can toggle basic risk parameters per run

### Phase 4 - Persistence model

1. Create entities and repositories for:
   - simulation runs
   - trades
   - equity points
2. Add PostgreSQL profile for persistent storage.
3. Keep H2 profile for local quick-start development.

**Exit criteria**

- each simulation run is saved and queryable by ID
- local dev still works without external DB setup

### Phase 5 - Backtest history and retrieval APIs

1. Add endpoints:
   - list runs (paged/filterable)
   - fetch run details (metrics/trades/equity)
   - compare two runs
2. Add run metadata:
   - symbol
   - strategy
   - parameters
   - created timestamp

**Exit criteria**

- user can revisit prior runs and compare outcomes
- run provenance is reproducible from stored parameters

### Phase 6 - Analysis features

1. Expand metrics:
   - profit factor
   - expectancy
   - average win / average loss
   - exposure time
2. Add parameter sweep endpoint for simple grid search.
3. Rank and return best parameter combinations by selected metric.

**Exit criteria**

- user can evaluate strategy quality beyond return-only metrics
- user can perform controlled parameter experiments

### Phase 7 - Quality and reliability hardening

1. Increase unit tests for engine/strategies/risk rules.
2. Add integration tests for REST + persistence (Testcontainers PostgreSQL).
3. Add global API error model and consistent response schema.
4. Add basic performance test for larger candle datasets.

**Exit criteria**

- deterministic test coverage around core simulation rules
- integration tests verify real DB behavior in CI-like conditions

### Phase 8 - UX improvements (still lightweight frontend)

1. Add CSV upload in UI.
2. Add saved-runs table and run detail view.
3. Add strategy comparison chart view (multiple equity curves overlay).
4. Keep frontend framework-free unless UI complexity forces upgrade.

**Exit criteria**

- users can execute, save, retrieve, and compare runs from browser
- no curl/Postman required for normal usage

### Phase 9 - Deployment and portfolio polish

1. Dockerize app (and compose file for app + PostgreSQL).
2. Add production-grade README sections:
   - architecture diagram
   - API examples
   - local/dev/prod profiles
3. Add demo assets:
   - screenshots/GIF
   - sample datasets

**Exit criteria**

- project runs with one documented setup path
- repo is portfolio-ready for internship applications

## 6) Definition of "finished project"

The final project is complete when a user can:

1. upload historical data
2. run multiple strategies with configurable risk controls
3. persist and retrieve backtest runs
4. compare runs and parameter experiments in UI
5. reproduce results reliably from stored configuration

## 7) Interview value talking points

When this project matures, you can discuss:

- designing extensible architecture for strategy simulation
- trade-offs of event-driven vs batch simulation loops
- testing deterministic financial logic
- API + persistence boundaries
- incremental delivery from backend MVP to full-stack product
