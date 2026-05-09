package com.tradingsim.infrastructure.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingsim.domain.Candle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class AlphaVantageMarketDataProvider implements MarketDataProvider {

    private static final String PROVIDER_NAME = "alphavantage";
    private static final String SUPPORTED_INTERVAL = "1d";
    private static final String BASE_URL =
            "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&datatype=csv&apikey=%s";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    public AlphaVantageMarketDataProvider(@Value("${market-data.alphavantage.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public List<Candle> fetchCandles(String symbol, LocalDate startDate, LocalDate endDate, String interval) {
        if (!SUPPORTED_INTERVAL.equalsIgnoreCase(interval)) {
            throw new IllegalArgumentException("Alpha Vantage currently supports only interval '1d'.");
        }
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Alpha Vantage API key is required. Set 'market-data.alphavantage.api-key' (or ALPHA_VANTAGE_API_KEY env var)."
            );
        }

        String cleanedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        if (cleanedSymbol.isBlank()) {
            throw new IllegalArgumentException("Symbol is required for market data fetch.");
        }

        String encodedSymbol = URLEncoder.encode(cleanedSymbol, StandardCharsets.UTF_8);
        String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        String url = String.format(BASE_URL, encodedSymbol, encodedKey);

        String body = downloadCsv(url);
        List<Candle> candles = parseCsv(body, startDate, endDate);

        if (candles.isEmpty()) {
            throw new IllegalArgumentException("No market data found for symbol '" + symbol
                    + "' in the requested date range. Free Alpha Vantage daily endpoint returns compact data"
                    + " (recent ~100 trading days). Try a more recent start date or use a premium plan.");
        }

        candles.sort(Comparator.comparing(Candle::timestamp));
        return candles;
    }

    private String downloadCsv(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("Market data provider returned status " + response.statusCode() + ".");
            }
            String body = response.body();
            if (body.trim().startsWith("{")) {
                throw new IllegalArgumentException(deriveProviderErrorMessage(body));
            }
            return body;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to fetch market data: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Market data request was interrupted.");
        }
    }

    String deriveProviderErrorMessage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String[] keys = {"Error Message", "Note", "Information", "message"};
            for (String key : keys) {
                if (root.has(key) && !root.get(key).asText().isBlank()) {
                    return "Alpha Vantage response: " + root.get(key).asText();
                }
            }
        } catch (Exception ignored) {
            // Fall back to a generic user-facing message below.
        }
        return "Unexpected response from Alpha Vantage. Check API key, symbol, and rate limits.";
    }

    private List<Candle> parseCsv(String csvContent, LocalDate startDate, LocalDate endDate) {
        String[] lines = csvContent.split("\\R");
        if (lines.length <= 1) {
            throw new IllegalArgumentException("Market data provider returned empty CSV content.");
        }
        String header = lines[0].toLowerCase(Locale.ROOT);
        if (!header.startsWith("timestamp,open,high,low,close")) {
            throw new IllegalArgumentException("Unexpected market data CSV format from provider.");
        }
        String[] headerParts = lines[0].split(",", -1);
        int volumeIndex = findVolumeIndex(headerParts);
        if (volumeIndex < 0) {
            throw new IllegalArgumentException("Market data CSV is missing required 'volume' column.");
        }

        List<Candle> candles = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split(",", -1);
            if (parts.length <= volumeIndex) {
                continue;
            }

            LocalDate date = LocalDate.parse(parts[0].trim());
            if (date.isBefore(startDate) || date.isAfter(endDate)) {
                continue;
            }

            BigDecimal open = new BigDecimal(parts[1].trim());
            BigDecimal high = new BigDecimal(parts[2].trim());
            BigDecimal low = new BigDecimal(parts[3].trim());
            BigDecimal close = new BigDecimal(parts[4].trim());
            long volume = Long.parseLong(parts[volumeIndex].trim());

            LocalDateTime timestamp = date.atTime(9, 30);
            candles.add(new Candle(timestamp, open, high, low, close, volume));
        }
        return candles;
    }

    private int findVolumeIndex(String[] headerParts) {
        for (int i = 0; i < headerParts.length; i++) {
            if ("volume".equalsIgnoreCase(headerParts[i].trim())) {
                return i;
            }
        }
        return -1;
    }
}
