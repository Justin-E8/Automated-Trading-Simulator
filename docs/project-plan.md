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

## 5) Milestone roadmap

### Milestone A (this setup)

- repository scaffold
- Spring Boot app boots
- package structure in place
- project plan + architecture docs

### Milestone B

- CSV ingestion
- simple strategy interface
- first strategy (SMA crossover)
- simulation engine with buy/sell actions

### Milestone C

- performance metrics
- persisted simulation run records
- REST endpoints to trigger/retrieve runs

### Milestone D

- robustness:
  - validation
  - error handling
  - edge-case tests
- Dockerized local environment

### Milestone E

- optional frontend dashboard for visualization
- advanced strategy/risk controls

## 6) Interview value talking points

When this project matures, you can discuss:

- designing extensible architecture for strategy simulation
- trade-offs of event-driven vs batch simulation loops
- testing deterministic financial logic
- API + persistence boundaries
- incremental delivery from backend MVP to full-stack product
