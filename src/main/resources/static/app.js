const statusMessage = document.getElementById("statusMessage");
const candlesInput = document.getElementById("candlesInput");
const tradesTableBody = document.getElementById("tradesTableBody");
const rawResponse = document.getElementById("rawResponse");
const equityCanvas = document.getElementById("equityCanvas");

function setStatus(message, isError = false) {
  statusMessage.textContent = message;
  statusMessage.style.color = isError ? "#b91c1c" : "#1f2937";
}

function formatNumber(value, digits = 2) {
  return Number(value).toLocaleString(undefined, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  });
}

async function loadSampleCandles() {
  setStatus("Loading sample candles...");
  try {
    const response = await fetch("/api/v1/simulations/sample-candles");
    if (!response.ok) {
      throw new Error("Failed to fetch sample candles.");
    }
    const candles = await response.json();
    candlesInput.value = JSON.stringify(candles, null, 2);
    setStatus(`Loaded ${candles.length} sample candles.`);
  } catch (error) {
    setStatus(error.message, true);
  }
}

function buildRequestPayload() {
  const candles = JSON.parse(candlesInput.value);
  if (!Array.isArray(candles) || candles.length === 0) {
    throw new Error("Candles JSON must be a non-empty array.");
  }

  return {
    symbol: document.getElementById("symbol").value.trim(),
    initialCash: Number(document.getElementById("initialCash").value),
    quantityPerTrade: Number(document.getElementById("quantityPerTrade").value),
    feeBps: Number(document.getElementById("feeBps").value),
    shortWindow: Number(document.getElementById("shortWindow").value),
    longWindow: Number(document.getElementById("longWindow").value),
    candles
  };
}

async function runBacktest() {
  setStatus("Running backtest...");
  try {
    const payload = buildRequestPayload();
    const response = await fetch("/api/v1/simulations/backtest", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    const body = await response.json();
    if (!response.ok) {
      throw new Error(body.error || "Backtest request failed.");
    }

    renderResult(body);
    setStatus("Backtest complete.");
  } catch (error) {
    setStatus(error.message, true);
  }
}

function renderResult(result) {
  document.getElementById("metricStrategy").textContent = result.strategyName;
  document.getElementById("metricStartCash").textContent = `$${formatNumber(result.startingCash)}`;
  document.getElementById("metricEndEquity").textContent = `$${formatNumber(result.endingEquity)}`;
  document.getElementById("metricTotalReturn").textContent = formatNumber(result.metrics.totalReturnPct, 4);
  document.getElementById("metricMaxDrawdown").textContent = formatNumber(result.metrics.maxDrawdownPct, 4);
  document.getElementById("metricSharpe").textContent = formatNumber(result.metrics.sharpeRatio, 4);
  document.getElementById("metricWinRate").textContent = formatNumber(result.metrics.winRatePct, 4);
  document.getElementById("metricTradeCount").textContent = String(result.metrics.tradeCount);

  renderTrades(result.trades);
  renderEquityCurve(result.equityCurve);
  rawResponse.textContent = JSON.stringify(result, null, 2);
}

function renderTrades(trades) {
  if (!Array.isArray(trades) || trades.length === 0) {
    tradesTableBody.innerHTML = "<tr><td colspan=\"6\">No trades generated.</td></tr>";
    return;
  }

  tradesTableBody.innerHTML = "";
  trades.forEach((trade) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${trade.timestamp}</td>
      <td>${trade.side}</td>
      <td>${trade.quantity}</td>
      <td>${formatNumber(trade.price)}</td>
      <td>${formatNumber(trade.fee, 4)}</td>
      <td>${formatNumber(trade.realizedPnl, 4)}</td>
    `;
    tradesTableBody.appendChild(row);
  });
}

function renderEquityCurve(points) {
  const ctx = equityCanvas.getContext("2d");
  const width = equityCanvas.width;
  const height = equityCanvas.height;

  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = "#ffffff";
  ctx.fillRect(0, 0, width, height);

  if (!Array.isArray(points) || points.length < 2) {
    ctx.fillStyle = "#334155";
    ctx.font = "14px Arial";
    ctx.fillText("Need at least two data points to draw equity curve.", 24, 36);
    return;
  }

  const values = points.map((p) => Number(p.equity));
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;

  const paddingLeft = 55;
  const paddingRight = 25;
  const paddingTop = 20;
  const paddingBottom = 40;
  const plotWidth = width - paddingLeft - paddingRight;
  const plotHeight = height - paddingTop - paddingBottom;

  ctx.strokeStyle = "#cbd5e1";
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(paddingLeft, paddingTop);
  ctx.lineTo(paddingLeft, height - paddingBottom);
  ctx.lineTo(width - paddingRight, height - paddingBottom);
  ctx.stroke();

  ctx.fillStyle = "#334155";
  ctx.font = "12px Arial";
  ctx.fillText(`Max ${formatNumber(max, 2)}`, 8, paddingTop + 4);
  ctx.fillText(`Min ${formatNumber(min, 2)}`, 8, height - paddingBottom - 2);

  ctx.strokeStyle = "#2563eb";
  ctx.lineWidth = 2;
  ctx.beginPath();

  points.forEach((point, index) => {
    const x = paddingLeft + (index / (points.length - 1)) * plotWidth;
    const y = paddingTop + ((max - Number(point.equity)) / range) * plotHeight;
    if (index === 0) {
      ctx.moveTo(x, y);
    } else {
      ctx.lineTo(x, y);
    }
  });
  ctx.stroke();
}

document.getElementById("loadSampleButton").addEventListener("click", loadSampleCandles);
document.getElementById("runBacktestButton").addEventListener("click", runBacktest);

loadSampleCandles();
