# Automated Trading Simulator - Project Plan

## Project objective

Build a backend-first simulation platform that is strong enough to discuss in internship interviews as a real engineering system, not just a code exercise.

## Current status

The core portfolio baseline is complete:

- CSV ingestion + validation
- multi-strategy execution (SMA crossover + mean reversion)
- risk and execution controls
- saved-run persistence
- run history and run comparison APIs
- parameter sweep endpoint
- consistent API error contracts
- automated test coverage (unit, integration, performance baseline)
- browser UI for end-to-end usage
- Dockerized runtime with PostgreSQL

## Functional scope

Users can:

1. Import historical OHLCV candles from CSV (including Yahoo exports)
2. Run backtests with configurable strategy and risk parameters
3. Inspect trades, equity curve, and expanded performance metrics
4. Save runs and retrieve them later
5. Compare two runs and inspect metric deltas
6. Run parameter sweeps and rank configurations by objective

## Non-functional scope

- deterministic and reproducible simulation behavior
- clean layered architecture with domain/engine separation
- backend logic covered by automated tests
- deployable locally with consistent runtime behavior

## Architecture summary

- `domain` - market and trade primitives
- `strategy` - strategy interfaces, configs, and implementations
- `engine` - simulation loop, risk exits, metrics calculation
- `application` - orchestration and persistence mapping
- `api` - REST endpoints and DTO contracts
- `infrastructure` - CSV parser and JPA repositories

## Runtime profiles

- **Local quick mode**: Spring Boot + H2 in-memory (`./mvnw spring-boot:run`)
- **Persistent mode**: Docker Compose + PostgreSQL (`docker compose up --build`)

## Quality strategy

- unit tests for strategy/engine rules
- integration tests for API and persistence workflows
- Testcontainers PostgreSQL coverage where Docker is available
- baseline performance test for larger candle sets
- standardized API error response schema for all endpoints

## Portfolio positioning

This project demonstrates:

- systems thinking (domain modeling + orchestration layers)
- API contract discipline (validation + error schema)
- simulation/risk logic implementation
- persistence and data lifecycle design
- practical testing strategy and deployment packaging

## Optional next enhancements

1. CI workflow for automated test runs on pull requests
2. exportable sweep and comparison results (CSV/JSON download)
3. richer visual analytics (multi-run overlays, drawdown chart)
4. authentication and per-user run ownership
