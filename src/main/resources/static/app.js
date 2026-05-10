const statusMessage = document.getElementById("statusMessage");
const candlesInput = document.getElementById("candlesInput");
const tradesTableBody = document.getElementById("tradesTableBody");
const equityCanvas = document.getElementById("equityCanvas");
const csvFileInput = document.getElementById("csvFileInput");
const strategyInput = document.getElementById("strategy");
const smaParams = document.getElementById("smaParams");
const meanReversionParams = document.getElementById("meanReversionParams");
const sweepSmaParams = document.getElementById("sweepSmaParams");
const sweepMeanReversionParams = document.getElementById("sweepMeanReversionParams");
const sweepResultsBody = document.getElementById("sweepResultsBody");

function ensureElement(element, name) {
  if (!element) {
    throw new Error(`UI setup error: missing element '${name}'. Refresh the page and try again.`);
  }
  return element;
}

function parseJsonOrThrow(text, fallbackMessage) {
  if (!text) {
    return {};
  }
  try {
    return JSON.parse(text);
  } catch (error) {
    throw new Error(fallbackMessage);
  }
}

async function readResponseBody(response, fallbackMessage) {
  const text = await response.text();
  return parseJsonOrThrow(text, fallbackMessage);
}

function setStatus(message, type = "info") {
  const banner = ensureElement(statusMessage, "statusMessage");
  banner.textContent = message;
  banner.classList.remove("status-info", "status-success", "status-error");
  banner.classList.add(`status-${type}`);
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
    strategy: ensureElement(strategyInput, "strategy").value,
    initialCash: Number(document.getElementById("initialCash").value),
    quantityPerTrade: Number(document.getElementById("quantityPerTrade").value),
    feeBps: Number(document.getElementById("feeBps").value),
    slippageBps: Number(document.getElementById("slippageBps").value),
    maxPositionSize: Number(document.getElementById("maxPositionSize").value),
    maxHoldingCandles: Number(document.getElementById("maxHoldingCandles").value),
    stopLossPct: Number(document.getElementById("stopLossPct").value),
    takeProfitPct: Number(document.getElementById("takeProfitPct").value),
    shortWindow: Number(document.getElementById("shortWindow").value),
    longWindow: Number(document.getElementById("longWindow").value),
    meanReversionWindow: Number(document.getElementById("meanReversionWindow").value),
    meanReversionThresholdPct: Number(document.getElementById("meanReversionThresholdPct").value)
  };
}

function selectedCsvFile() {
  const fileInput = ensureElement(csvFileInput, "csvFileInput");
  const file = fileInput.files && fileInput.files[0];
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
  formData.append("slippageBps", String(params.slippageBps));
  formData.append("maxPositionSize", String(params.maxPositionSize));
  formData.append("maxHoldingCandles", String(params.maxHoldingCandles));
  formData.append("stopLossPct", String(params.stopLossPct));
  formData.append("takeProfitPct", String(params.takeProfitPct));
  formData.append("shortWindow", String(params.shortWindow));
  formData.append("longWindow", String(params.longWindow));
  formData.append("strategy", params.strategy);
  formData.append("meanReversionWindow", String(params.meanReversionWindow));
  formData.append("meanReversionThresholdPct", String(params.meanReversionThresholdPct));
  return formData;
}

function buildSweepFormData() {
  const params = baseParameters();
  const formData = new FormData();
  formData.append("file", selectedCsvFile());
  formData.append("symbol", params.symbol);
  formData.append("strategy", params.strategy);
  formData.append("initialCash", String(params.initialCash));
  formData.append("quantityPerTrade", String(params.quantityPerTrade));
  formData.append("feeBps", String(params.feeBps));
  formData.append("slippageBps", String(params.slippageBps));
  formData.append("stopLossPct", String(params.stopLossPct));
  formData.append("takeProfitPct", String(params.takeProfitPct));
  formData.append("maxPositionSize", String(params.maxPositionSize));
  formData.append("maxHoldingCandles", String(params.maxHoldingCandles));
  formData.append("optimizeFor", String(document.getElementById("sweepOptimizeFor").value));
  formData.append("maxResults", String(document.getElementById("sweepMaxResults").value));
  formData.append("shortWindowStart", String(document.getElementById("sweepShortStart").value));
  formData.append("shortWindowEnd", String(document.getElementById("sweepShortEnd").value));
  formData.append("shortWindowStep", String(document.getElementById("sweepShortStep").value));
  formData.append("longWindowStart", String(document.getElementById("sweepLongStart").value));
  formData.append("longWindowEnd", String(document.getElementById("sweepLongEnd").value));
  formData.append("longWindowStep", String(document.getElementById("sweepLongStep").value));
  formData.append("meanReversionWindowStart", String(document.getElementById("sweepMeanWindowStart").value));
  formData.append("meanReversionWindowEnd", String(document.getElementById("sweepMeanWindowEnd").value));
  formData.append("meanReversionWindowStep", String(document.getElementById("sweepMeanWindowStep").value));
  formData.append("meanReversionThresholdStartPct", String(document.getElementById("sweepMeanThresholdStart").value));
  formData.append("meanReversionThresholdEndPct", String(document.getElementById("sweepMeanThresholdEnd").value));
  formData.append("meanReversionThresholdStepPct", String(document.getElementById("sweepMeanThresholdStep").value));
  return formData;
}

function initializeCsvPreview() {
  // CSV-only workflow no longer preloads sample JSON.
  ensureElement(candlesInput, "candlesInput").value = "Upload a CSV and click Preview CSV.";
  syncStrategyControls();
}

function syncStrategyControls() {
  const strategy = ensureElement(strategyInput, "strategy").value;
  if (strategy === "mean-reversion") {
    ensureElement(smaParams, "smaParams").classList.add("hidden");
    ensureElement(meanReversionParams, "meanReversionParams").classList.remove("hidden");
    ensureElement(sweepSmaParams, "sweepSmaParams").classList.add("hidden");
    ensureElement(sweepMeanReversionParams, "sweepMeanReversionParams").classList.remove("hidden");
    return;
  }
  ensureElement(smaParams, "smaParams").classList.remove("hidden");
  ensureElement(meanReversionParams, "meanReversionParams").classList.add("hidden");
  ensureElement(sweepSmaParams, "sweepSmaParams").classList.remove("hidden");
  ensureElement(sweepMeanReversionParams, "sweepMeanReversionParams").classList.add("hidden");
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
    const body = await readResponseBody(response, "Server returned invalid preview response.");
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
    const body = await readResponseBody(response, "Server returned invalid backtest response.");
    if (!response.ok) {
      throw new Error(body.error || "CSV backtest failed.");
    }

    renderResult(body);
    const runLabel = body.runId !== undefined && body.runId !== null ? ` Run ID: ${body.runId}.` : "";
    setStatus(`CSV backtest complete.${runLabel}`, "success");
  } catch (error) {
    setStatus(error.message, "error");
  }
}

async function runParameterSweep() {
  setStatus("Running parameter sweep...");
  try {
    const response = await fetch("/api/v1/simulations/csv/sweep", {
      method: "POST",
      body: buildSweepFormData()
    });
    const body = await readResponseBody(response, "Server returned invalid sweep response.");
    if (!response.ok) {
      throw new Error(body.error || "Parameter sweep failed.");
    }

    renderSweepResults(body.results || []);
    const bestScore = body.bestResult ? formatNumber(body.bestResult.objectiveScore, 4) : "n/a";
    setStatus(
      `Sweep complete. Evaluated ${body.evaluatedCombinations} combinations; showing ${body.returnedCombinations}. Best score: ${bestScore}.`,
      "success"
    );
  } catch (error) {
    setStatus(error.message, "error");
  }
}

function renderResult(result) {
  document.getElementById("metricRunId").textContent = result.runId ?? "-";
  document.getElementById("metricStrategy").textContent = result.strategyName;
  document.getElementById("metricStartCash").textContent = `$${formatNumber(result.startingCash)}`;
  document.getElementById("metricEndEquity").textContent = `$${formatNumber(result.endingEquity)}`;
  document.getElementById("metricTotalReturn").textContent = formatNumber(result.metrics.totalReturnPct, 4);
  document.getElementById("metricMaxDrawdown").textContent = formatNumber(result.metrics.maxDrawdownPct, 4);
  document.getElementById("metricSharpe").textContent = formatNumber(result.metrics.sharpeRatio, 4);
  document.getElementById("metricWinRate").textContent = formatNumber(result.metrics.winRatePct, 4);
  document.getElementById("metricProfitFactor").textContent = formatNumber(result.metrics.profitFactor, 4);
  document.getElementById("metricExpectancy").textContent = formatNumber(result.metrics.expectancy, 4);
  document.getElementById("metricAverageWin").textContent = formatNumber(result.metrics.averageWin, 4);
  document.getElementById("metricAverageLoss").textContent = formatNumber(result.metrics.averageLoss, 4);
  document.getElementById("metricExposureTime").textContent = formatNumber(result.metrics.exposureTimePct, 4);
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
    ensureElement(tradesTableBody, "tradesTableBody").innerHTML = "<tr><td colspan=\"6\">No trades generated.</td></tr>";
    return;
  }

  ensureElement(tradesTableBody, "tradesTableBody").innerHTML = "";
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
    ensureElement(tradesTableBody, "tradesTableBody").appendChild(row);
  });
}

function renderSweepResults(results) {
  if (!Array.isArray(results) || results.length === 0) {
    ensureElement(sweepResultsBody, "sweepResultsBody").innerHTML = "<tr><td colspan=\"6\">No sweep results returned.</td></tr>";
    return;
  }
  ensureElement(sweepResultsBody, "sweepResultsBody").innerHTML = "";
  results.forEach((result) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${result.rank}</td>
      <td>${configurationLabel(result.parameters)}</td>
      <td>${formatNumber(result.objectiveScore, 4)}</td>
      <td>${formatNumber(result.metrics.totalReturnPct, 4)}</td>
      <td>${formatNumber(result.metrics.sharpeRatio, 4)}</td>
      <td>${result.metrics.tradeCount}</td>
    `;
    ensureElement(sweepResultsBody, "sweepResultsBody").appendChild(row);
  });
}

function configurationLabel(parameters) {
  if (parameters.shortWindow !== null && parameters.longWindow !== null) {
    return `SMA(${parameters.shortWindow}, ${parameters.longWindow})`;
  }
  if (parameters.meanReversionWindow !== null && parameters.meanReversionThresholdPct !== null) {
    return `MeanRev(${parameters.meanReversionWindow}, ${parameters.meanReversionThresholdPct}%)`;
  }
  return "Unknown";
}

function renderEquityCurve(points) {
  const canvas = ensureElement(equityCanvas, "equityCanvas");
  const ctx = canvas.getContext("2d");
  const width = canvas.width;
  const height = canvas.height;

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
document.getElementById("runSweepButton").addEventListener("click", runParameterSweep);
ensureElement(strategyInput, "strategy").addEventListener("change", () => {
  syncStrategyControls();
  setStatus(`Strategy selected: ${strategyInput.value}`, "info");
});
ensureElement(csvFileInput, "csvFileInput").addEventListener("change", () => {
  try {
    const file = selectedCsvFile();
    setStatus(`Selected CSV: ${file.name}`, "info");
  } catch (error) {
    setStatus("Choose a CSV file first.", "error");
  }
});

initializeCsvPreview();
