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
const runHistoryBody = document.getElementById("runHistoryBody");
const loadRunsButton = document.getElementById("loadRunsButton");
const historyPrevButton = document.getElementById("historyPrevButton");
const historyNextButton = document.getElementById("historyNextButton");
const historyPageLabel = document.getElementById("historyPageLabel");
const leftRunIdInput = document.getElementById("leftRunIdInput");
const rightRunIdInput = document.getElementById("rightRunIdInput");
const compareRunsButton = document.getElementById("compareRunsButton");
const comparisonSummary = document.getElementById("comparisonSummary");
const infoPopover = document.getElementById("infoPopover");

const historyState = {
  page: 0,
  totalPages: 0
};
let activeInfoButton = null;

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

function extractErrorMessage(body, fallbackMessage) {
  if (!body || typeof body !== "object") {
    return fallbackMessage;
  }
  return body.error || body.message || fallbackMessage;
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

function formatSignedNumber(value, digits = 4, includeDollar = false) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return includeDollar ? "$0.0000" : "0.0000";
  }
  const sign = number > 0 ? "+" : "";
  const formatted = formatNumber(number, digits);
  return includeDollar ? `${sign}$${formatted}` : `${sign}${formatted}`;
}

function toIsoDateTime(value) {
  if (!value) {
    return "-";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

function hideInfoPopover() {
  const popover = ensureElement(infoPopover, "infoPopover");
  popover.classList.add("hidden");
  popover.textContent = "";
  activeInfoButton = null;
}

function showInfoPopover(button) {
  const popover = ensureElement(infoPopover, "infoPopover");
  const message = button.dataset.info || "No additional info available.";
  popover.textContent = message;
  popover.classList.remove("hidden");

  const rect = button.getBoundingClientRect();
  const viewportWidth = window.innerWidth;
  const maxLeft = Math.max(12, viewportWidth - popover.offsetWidth - 12);
  const left = Math.min(maxLeft, Math.max(12, rect.left + window.scrollX - (popover.offsetWidth / 2) + (rect.width / 2)));
  const top = rect.bottom + window.scrollY + 8;

  popover.style.left = `${left}px`;
  popover.style.top = `${top}px`;
  activeInfoButton = button;
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
  renderComparisonPlaceholder();
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
      throw new Error(extractErrorMessage(body, "CSV preview failed."));
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
      throw new Error(extractErrorMessage(body, "CSV backtest failed."));
    }

    renderResult(body);
    await loadSavedRuns(0);
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
      throw new Error(extractErrorMessage(body, "Parameter sweep failed."));
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

async function loadSavedRuns(pageOverride = null) {
  const pageSize = Number(document.getElementById("historyPageSize").value);
  const symbol = document.getElementById("historySymbolFilter").value.trim();
  const strategy = document.getElementById("historyStrategyFilter").value.trim();
  const requestedPage = pageOverride === null ? historyState.page : pageOverride;
  const page = Number.isFinite(requestedPage) ? Math.max(0, requestedPage) : 0;
  if (!Number.isFinite(pageSize) || pageSize < 1 || pageSize > 100) {
    setStatus("History page size must be between 1 and 100.", "error");
    return;
  }

  setStatus("Loading saved runs...");
  try {
    const query = new URLSearchParams({
      page: String(page),
      size: String(pageSize),
      symbol,
      strategy
    });
    const response = await fetch(`/api/v1/simulations/runs?${query.toString()}`);
    const body = await readResponseBody(response, "Server returned invalid run history response.");
    if (!response.ok) {
      throw new Error(extractErrorMessage(body, "Loading run history failed."));
    }

    historyState.page = body.page ?? 0;
    historyState.totalPages = body.totalPages ?? 0;
    renderRunHistory(body.runs || []);
    updateHistoryPaginationState();
    const count = Array.isArray(body.runs) ? body.runs.length : 0;
    setStatus(`Loaded ${count} saved run(s).`, "success");
  } catch (error) {
    setStatus(error.message, "error");
  }
}

async function loadRunById(runId) {
  setStatus(`Loading run ${runId}...`);
  try {
    const response = await fetch(`/api/v1/simulations/runs/${runId}`);
    const body = await readResponseBody(response, "Server returned invalid run response.");
    if (!response.ok) {
      throw new Error(extractErrorMessage(body, `Loading run ${runId} failed.`));
    }
    renderResult(body);
    setStatus(`Loaded saved run ${runId}.`, "success");
  } catch (error) {
    setStatus(error.message, "error");
  }
}

async function compareRuns() {
  const leftRunId = Number(ensureElement(leftRunIdInput, "leftRunIdInput").value);
  const rightRunId = Number(ensureElement(rightRunIdInput, "rightRunIdInput").value);
  if (!Number.isFinite(leftRunId) || leftRunId <= 0 || !Number.isFinite(rightRunId) || rightRunId <= 0) {
    setStatus("Enter valid positive IDs for both compare run fields.", "error");
    return;
  }
  if (leftRunId === rightRunId) {
    setStatus("Choose two different run IDs to compare.", "error");
    return;
  }

  setStatus(`Comparing runs ${leftRunId} and ${rightRunId}...`);
  try {
    const query = new URLSearchParams({
      leftRunId: String(leftRunId),
      rightRunId: String(rightRunId)
    });
    const response = await fetch(`/api/v1/simulations/runs/compare?${query.toString()}`);
    const body = await readResponseBody(response, "Server returned invalid comparison response.");
    if (!response.ok) {
      throw new Error(extractErrorMessage(body, "Run comparison failed."));
    }
    renderComparison(body);
    setStatus("Run comparison loaded.", "success");
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

function renderRunHistory(runs) {
  if (!Array.isArray(runs) || runs.length === 0) {
    ensureElement(runHistoryBody, "runHistoryBody").innerHTML = "<tr><td colspan=\"7\">No saved runs found for this filter.</td></tr>";
    return;
  }
  ensureElement(runHistoryBody, "runHistoryBody").innerHTML = "";
  runs.forEach((run) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${run.runId}</td>
      <td>${toIsoDateTime(run.createdAt)}</td>
      <td>${run.symbol}</td>
      <td>${run.strategyName}</td>
      <td>${formatNumber(run.totalReturnPct, 4)}</td>
      <td>$${formatNumber(run.endingEquity, 2)}</td>
      <td>
        <div class="button-row">
          <button class="secondary-button history-load-btn" type="button" data-run-id="${run.runId}">Load</button>
          <button class="secondary-button history-left-btn" type="button" data-run-id="${run.runId}">Set Left</button>
          <button class="secondary-button history-right-btn" type="button" data-run-id="${run.runId}">Set Right</button>
        </div>
      </td>
    `;
    ensureElement(runHistoryBody, "runHistoryBody").appendChild(row);
  });
}

function renderComparisonPlaceholder() {
  ensureElement(comparisonSummary, "comparisonSummary").textContent = "Choose two run IDs and click Compare Runs.";
}

function renderComparison(comparison) {
  if (!comparison || !comparison.leftRun || !comparison.rightRun || !comparison.delta) {
    renderComparisonPlaceholder();
    return;
  }
  const delta = comparison.delta;
  ensureElement(comparisonSummary, "comparisonSummary").innerHTML = `
    <strong>Left:</strong> #${comparison.leftRun.runId} (${comparison.leftRun.strategyName}) |
    <strong>Right:</strong> #${comparison.rightRun.runId} (${comparison.rightRun.strategyName})
    <ul>
      <li>End Equity Delta: ${formatSignedNumber(delta.endingEquityDelta, 4, true)}</li>
      <li>Total Return % Delta: ${formatSignedNumber(delta.totalReturnPctDelta)}</li>
      <li>Max Drawdown % Delta: ${formatSignedNumber(delta.maxDrawdownPctDelta)}</li>
      <li>Sharpe Delta: ${formatSignedNumber(delta.sharpeRatioDelta)}</li>
      <li>Win Rate % Delta: ${formatSignedNumber(delta.winRatePctDelta)}</li>
      <li>Profit Factor Delta: ${formatSignedNumber(delta.profitFactorDelta)}</li>
      <li>Expectancy Delta: ${formatSignedNumber(delta.expectancyDelta)}</li>
      <li>Average Win Delta: ${formatSignedNumber(delta.averageWinDelta)}</li>
      <li>Average Loss Delta: ${formatSignedNumber(delta.averageLossDelta)}</li>
      <li>Exposure Time % Delta: ${formatSignedNumber(delta.exposureTimePctDelta)}</li>
      <li>Trade Count Delta: ${formatSignedNumber(delta.tradeCountDelta, 0)}</li>
    </ul>
  `;
}

function updateHistoryPaginationState() {
  const pageNumber = (historyState.page || 0) + 1;
  const totalPages = historyState.totalPages || 0;
  ensureElement(historyPageLabel, "historyPageLabel").textContent = totalPages === 0
    ? "Page 1 / 1"
    : `Page ${pageNumber} / ${totalPages}`;
  ensureElement(historyPrevButton, "historyPrevButton").disabled = historyState.page <= 0;
  ensureElement(historyNextButton, "historyNextButton").disabled = totalPages === 0 || historyState.page >= totalPages - 1;
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
ensureElement(loadRunsButton, "loadRunsButton").addEventListener("click", () => loadSavedRuns(0));
ensureElement(historyPrevButton, "historyPrevButton").addEventListener("click", () => {
  if (historyState.page > 0) {
    loadSavedRuns(historyState.page - 1);
  }
});
ensureElement(historyNextButton, "historyNextButton").addEventListener("click", () => {
  if (historyState.page < historyState.totalPages - 1) {
    loadSavedRuns(historyState.page + 1);
  }
});
ensureElement(compareRunsButton, "compareRunsButton").addEventListener("click", compareRuns);
ensureElement(runHistoryBody, "runHistoryBody").addEventListener("click", (event) => {
  const button = event.target.closest("button");
  if (!button) {
    return;
  }
  const runId = Number(button.dataset.runId);
  if (!Number.isFinite(runId) || runId <= 0) {
    return;
  }
  if (button.classList.contains("history-load-btn")) {
    loadRunById(runId);
    return;
  }
  if (button.classList.contains("history-left-btn")) {
    ensureElement(leftRunIdInput, "leftRunIdInput").value = String(runId);
    setStatus(`Left compare run set to ${runId}.`, "info");
    return;
  }
  if (button.classList.contains("history-right-btn")) {
    ensureElement(rightRunIdInput, "rightRunIdInput").value = String(runId);
    setStatus(`Right compare run set to ${runId}.`, "info");
  }
});
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
document.addEventListener("click", (event) => {
  const trigger = event.target.closest(".info-trigger");
  if (trigger) {
    event.preventDefault();
    if (activeInfoButton === trigger) {
      hideInfoPopover();
      return;
    }
    showInfoPopover(trigger);
    return;
  }
  if (activeInfoButton && !event.target.closest("#infoPopover")) {
    hideInfoPopover();
  }
});
window.addEventListener("resize", hideInfoPopover);
window.addEventListener("scroll", hideInfoPopover, true);
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    hideInfoPopover();
  }
});

initializeCsvPreview();
updateHistoryPaginationState();
loadSavedRuns(0);
