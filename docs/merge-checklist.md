# Merge Checklist (Feature Branch -> `main`)

Use this checklist before merging `cursor/yfinance-csv-helper-script-0827` into `main`.

## 1) Sync and verify branch state

1. Confirm you are on the feature branch:
   ```bash
   git checkout cursor/yfinance-csv-helper-script-0827
   ```
2. Pull latest remote updates:
   ```bash
   git pull origin cursor/yfinance-csv-helper-script-0827
   ```
3. Confirm working tree is clean:
   ```bash
   git status
   ```

## 2) Re-run quality checks

1. Run tests:
   ```bash
   ./mvnw test
   ```
2. Optional quick runtime smoke check:
   - start app with `./mvnw spring-boot:run`
   - open `http://localhost:8080`
   - upload CSV -> Preview CSV -> Run Backtest from CSV

## 3) Review PR content

1. Confirm PR title/body summarize:
   - CSV-only workflow
   - UI cleanup and error-banner improvements
   - `data/` ignored and no tracked CSV artifacts
2. Confirm commit list is expected and readable.
3. Confirm no accidental file additions under `data/`.

## 4) Merge

1. Merge PR into `main` from GitHub.
2. After merge, sync local `main`:
   ```bash
   git checkout main
   git pull origin main
   ```

## 5) Optional cleanup

1. Delete remote feature branch after merge.
2. Delete local feature branch when no longer needed:
   ```bash
   git branch -d cursor/yfinance-csv-helper-script-0827
   ```
