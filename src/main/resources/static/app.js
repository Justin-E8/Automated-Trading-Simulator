const statusMessage = document.getElementById("statusMessage");
const candlesInput = document.getElementById("candlesInput");
const tradesTableBody = document.getElementById("tradesTableBody");
const equityCanvas = document.getElementById("equityCanvas");
const csvFileInput = document.getElementById("csvFileInput");

function setStatus(message, type = "info") {
  statusMessage.textContent = message;
  statusMessage.classList.remove("status-info", "status-success", "status-error");
  statusMessage.classList.add(`status-${type}`);
}

function formatNumber(value, digits = 2) {
  return Number(value).toLocaleString(undefined, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  });
}

function baseParameters() {
  return {
    symbol: document.getElementById("symbol").value.trim(),
    initialCash: Number(document.getElementById("initialCash").value),
    quantityPerTrade: Number(document.getElementById("quantityPerTrade").value),
    feeBps: Number(document.getElementById("feeBps").value),
    shortWindow: Number(document.getElementById("shortWindow").value),
    longWindow: Number(document.getElementById("longWindow").value)
  };
}

function selectedCsvFile() {
  const file = csvFileInput.files && csvFileInput.files[0];
  if (!file) {
    throw new Error("Choose a CSV file first.");
  }
  return file;
}

function buildCsvFormData() {
  const params = baseParameters();
  const formData = new FormData();
  formData.append("file", selectedCsvFile());
  formData.append("symbol", params.symbol);
  formData.append("initialCash", String(params.initialCash));
  formData.append("quantityPerTrade", String(params.quantityPerTrade));
  formData.append("feeBps", String(params.feeBps));
  formData.append("shortWindow", String(params.shortWindow));
  formData.append("longWindow", String(params.longWindow));
  return formData;
}

function initializeCsvPreview() {
  // CSV-only workflow no longer preloads sample JSON.
  candlesInput.value = "Upload a CSV and click Preview CSV.";
}

async function previewCsv() {
  setStatus("Previewing CSV...");
  try {
    const formData = new FormData();
    formData.append("file", selectedCsvFile());
    const response = await fetch("/api/v1/simulations/csv/preview", {
      method: "POST",
      body: formData
    });
    const body = await response.json();
    if (!response.ok) {
      throw new Error(body.error || "CSV preview failed.");
    }

    renderCsvPreview(body);
    setStatus("CSV preview ready.", "success");
  } catch (error) {
    setStatus(error.message, "error");
  }
}

async function runCsvBacktest() {
  setStatus("Running CSV backtest...");
  try {
    const response = await fetch("/api/v1/simulations/csv/backtest", {
      method: "POST",
      body: buildCsvFormData()
    });
    const body = await response.json();
    if (!response.ok) {
      throw new Error(body.error || "CSV backtest failed.");
    }

    renderResult(body);
    setStatus("CSV backtest complete.", "success");
  } catch (error) {
    setStatus(error.message, "error");
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
}

function renderCsvPreview(preview) {
  document.getElementById("csvPreviewCount").textContent = String(preview.candleCount);
  document.getElementById("csvPreviewStart").textContent = preview.startTimestamp;
  document.getElementById("csvPreviewEnd").textContent = preview.endTimestamp;
  document.getElementById("csvPreviewMinClose").textContent = formatNumber(preview.minClose, 4);
  document.getElementById("csvPreviewMaxClose").textContent = formatNumber(preview.maxClose, 4);

  if (Array.isArray(preview.sampleCandles) && preview.sampleCandles.length > 0) {
    candlesInput.value = JSON.stringify(preview.sampleCandles, null, 2);
  }
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

document.getElementById("previewCsvButton").addEventListener("click", previewCsv);
document.getElementById("runCsvBacktestButton").addEventListener("click", runCsvBacktest);

initializeCsvPreview();
