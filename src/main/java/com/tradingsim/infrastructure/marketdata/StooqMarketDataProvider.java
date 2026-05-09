package com.tradingsim.infrastructure.marketdata;

import com.tradingsim.domain.Candle;
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
public class StooqMarketDataProvider implements MarketDataProvider {

    private static final String PROVIDER_NAME = "stooq";
    private static final String BASE_URL = "https://stooq.com/q/d/l/?s=%s&i=d";
    private static final String SUPPORTED_INTERVAL = "1d";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public List<Candle> fetchCandles(String symbol, LocalDate startDate, LocalDate endDate, String interval) {
        if (!SUPPORTED_INTERVAL.equalsIgnoreCase(interval)) {
            throw new IllegalArgumentException("Stooq provider currently supports only interval '1d'.");
        }

        String resolvedSymbol = resolveSymbol(symbol);
        String encodedSymbol = URLEncoder.encode(resolvedSymbol, StandardCharsets.UTF_8);
        String url = String.format(BASE_URL, encodedSymbol);

        String csvContent = downloadCsv(url);
        List<Candle> candles = parseCsv(csvContent, startDate, endDate);

        if (candles.isEmpty()) {
            throw new IllegalArgumentException("No market data found for symbol '" + symbol
                    + "' in the requested date range.");
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
            return response.body();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to fetch market data: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Market data request was interrupted.");
        }
    }

    private List<Candle> parseCsv(String csvContent, LocalDate startDate, LocalDate endDate) {
        String[] lines = csvContent.split("\\R");
        if (lines.length <= 1) {
            throw new IllegalArgumentException("Market data provider returned empty CSV content.");
        }

        List<Candle> candles = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split(",", -1);
            if (parts.length < 6) {
                continue;
            }
            if (containsUnavailableValues(parts)) {
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
            long volume = Long.parseLong(parts[5].trim());

            LocalDateTime timestamp = date.atTime(9, 30);
            candles.add(new Candle(timestamp, open, high, low, close, volume));
        }

        return candles;
    }

    private boolean containsUnavailableValues(String[] parts) {
        for (String part : parts) {
            String normalized = part.trim().toUpperCase(Locale.ROOT);
            if ("N/D".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String resolveSymbol(String symbol) {
        String normalized = symbol.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Symbol is required for market data fetch.");
        }
        return normalized.contains(".") ? normalized : normalized + ".us";
    }
}
