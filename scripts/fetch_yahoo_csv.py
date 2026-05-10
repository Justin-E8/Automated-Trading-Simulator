#!/usr/bin/env python3
"""
Download Yahoo Finance historical candles and write a simulator-ready CSV.

Output columns:
Date,Open,High,Low,Close,Adj Close,Volume
"""

from __future__ import annotations

import argparse
import sys
from datetime import datetime
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fetch Yahoo historical data and save as CSV for simulator upload."
    )
    parser.add_argument(
        "--ticker",
        required=True,
        help="Ticker symbol, e.g. AAPL, MSFT, TSLA",
    )
    parser.add_argument(
        "--start",
        required=True,
        help="Start date in YYYY-MM-DD format (inclusive).",
    )
    parser.add_argument(
        "--end",
        required=True,
        help="End date in YYYY-MM-DD format (exclusive in yfinance).",
    )
    parser.add_argument(
        "--interval",
        default="1d",
        choices=["1d", "1wk", "1mo"],
        help="Candle interval. Default: 1d",
    )
    parser.add_argument(
        "--out",
        help="Output CSV path. If set, this overrides --out-dir.",
    )
    parser.add_argument(
        "--out-dir",
        default="data/generated-csv",
        help="Output directory for generated CSV files. Default: data/generated-csv",
    )
    return parser.parse_args()


def validate_date(date_value: str) -> str:
    try:
        datetime.strptime(date_value, "%Y-%m-%d")
    except ValueError as exc:
        raise ValueError(f"Invalid date '{date_value}'. Use YYYY-MM-DD format.") from exc
    return date_value


def main() -> int:
    args = parse_args()

    try:
        validate_date(args.start)
        validate_date(args.end)
    except ValueError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 2

    try:
        import yfinance as yf
        import pandas as pd
    except ImportError:
        print(
            "Missing dependency: yfinance.\n"
            "Install with: pip install yfinance pandas",
            file=sys.stderr,
        )
        return 2

    ticker = args.ticker.strip().upper()
    if not ticker:
        print("Error: ticker cannot be blank.", file=sys.stderr)
        return 2

    if args.out:
        output_path = Path(args.out)
    else:
        out_dir = Path(args.out_dir)
        output_path = out_dir / f"{ticker}-{args.start}-{args.end}.csv"

    output_path.parent.mkdir(parents=True, exist_ok=True)

    df = yf.download(
        ticker,
        start=args.start,
        end=args.end,
        interval=args.interval,
        auto_adjust=False,
        progress=False,
        group_by="column",
    )

    if df.empty:
        print(
            "No rows returned. Check ticker/date range/interval and try again.",
            file=sys.stderr,
        )
        return 1

    if getattr(df.columns, "nlevels", 1) > 1:
        # Newer yfinance may return MultiIndex columns like ('Close', 'AAPL').
        df.columns = [col[0] if isinstance(col, tuple) else col for col in df.columns]

    df = df.reset_index()
    if "Date" not in df.columns and "Datetime" in df.columns:
        df = df.rename(columns={"Datetime": "Date"})
    if "Date" in df.columns:
        df["Date"] = pd.to_datetime(df["Date"], errors="coerce").dt.strftime("%Y-%m-%d")

    required_columns = ["Date", "Open", "High", "Low", "Close", "Adj Close", "Volume"]
    if "Adj Close" not in df.columns and "Close" in df.columns:
        df["Adj Close"] = df["Close"]

    missing = [column for column in required_columns if column not in df.columns]
    if missing:
        print(
            "Unexpected Yahoo data format. Missing columns: " + ", ".join(missing),
            file=sys.stderr,
        )
        return 1

    df = df[required_columns]
    df = df.dropna(subset=["Date", "Open", "High", "Low", "Close", "Adj Close", "Volume"])
    if df.empty:
        print("No valid rows remained after normalization.", file=sys.stderr)
        return 1

    for numeric_col in ["Open", "High", "Low", "Close", "Adj Close"]:
        df[numeric_col] = pd.to_numeric(df[numeric_col], errors="coerce")
    df["Volume"] = pd.to_numeric(df["Volume"], errors="coerce")
    df = df.dropna(subset=["Open", "High", "Low", "Close", "Adj Close", "Volume"])
    if df.empty:
        print("No numeric rows remained after normalization.", file=sys.stderr)
        return 1

    df["Volume"] = df["Volume"].astype("int64")
    df.to_csv(output_path, index=False)
    print(f"Saved {len(df)} rows to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
