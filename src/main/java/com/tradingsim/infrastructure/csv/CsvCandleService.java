package com.tradingsim.infrastructure.csv;

import com.tradingsim.domain.Candle;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CsvCandleService {

    private static final List<String> REQUIRED_COLUMNS = List.of("timestamp", "open", "high", "low", "close", "volume");
    private static final DateTimeFormatter SPACE_SEPARATED_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<Candle> parseCandles(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required and cannot be empty.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = findNextNonEmptyLine(reader);
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty.");
            }

            List<String> headerTokens = splitCsvLine(headerLine);
            Map<String, Integer> headerIndex = headerIndexByName(headerTokens);
            validateRequiredColumns(headerIndex);

            List<Candle> candles = new ArrayList<>();
            String line;
            int lineNumber = 1;
            LocalDateTime previousTimestamp = null;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> values = splitCsvLine(line);
                Candle candle = parseCandleRow(values, headerIndex, lineNumber);

                if (previousTimestamp != null && !candle.timestamp().isAfter(previousTimestamp)) {
                    throw new IllegalArgumentException("Invalid timestamp order at CSV line " + lineNumber +
                            ". Timestamps must be strictly increasing.");
                }

                previousTimestamp = candle.timestamp();
                candles.add(candle);
            }

            if (candles.isEmpty()) {
                throw new IllegalArgumentException("CSV contains no candle rows.");
            }

            return candles;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read CSV file: " + exception.getMessage());
        }
    }

    public CsvPreviewSummary preview(MultipartFile file) {
        List<Candle> candles = parseCandles(file);

        BigDecimal minClose = candles.stream()
                .map(Candle::close)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxClose = candles.stream()
                .map(Candle::close)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<Candle> sample = candles.subList(0, Math.min(candles.size(), 5));

        return new CsvPreviewSummary(
                candles.size(),
                candles.get(0).timestamp(),
                candles.get(candles.size() - 1).timestamp(),
                minClose,
                maxClose,
                sample
        );
    }

    private Map<String, Integer> headerIndexByName(List<String> headerTokens) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headerTokens.size(); i++) {
            String normalized = normalizeToken(headerTokens.get(i)).toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                index.put(normalized, i);
            }
        }
        return index;
    }

    private void validateRequiredColumns(Map<String, Integer> headerIndex) {
        List<String> missing = REQUIRED_COLUMNS.stream()
                .filter(column -> !headerIndex.containsKey(column))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("CSV is missing required columns: " + String.join(", ", missing));
        }
    }

    private Candle parseCandleRow(List<String> values, Map<String, Integer> headerIndex, int lineNumber) {
        LocalDateTime timestamp = parseTimestamp(requiredValue(values, headerIndex, "timestamp", lineNumber), lineNumber);
        BigDecimal open = parsePositiveDecimal(requiredValue(values, headerIndex, "open", lineNumber), "open", lineNumber);
        BigDecimal high = parsePositiveDecimal(requiredValue(values, headerIndex, "high", lineNumber), "high", lineNumber);
        BigDecimal low = parsePositiveDecimal(requiredValue(values, headerIndex, "low", lineNumber), "low", lineNumber);
        BigDecimal close = parsePositiveDecimal(requiredValue(values, headerIndex, "close", lineNumber), "close", lineNumber);
        long volume = parseNonNegativeLong(requiredValue(values, headerIndex, "volume", lineNumber), "volume", lineNumber);

        if (low.compareTo(high) > 0) {
            throw new IllegalArgumentException("Invalid high/low values at CSV line " + lineNumber + ".");
        }
        if (high.compareTo(open.max(close)) < 0 || low.compareTo(open.min(close)) > 0) {
            throw new IllegalArgumentException("OHLC consistency check failed at CSV line " + lineNumber + ".");
        }

        return new Candle(timestamp, open, high, low, close, volume);
    }

    private String requiredValue(List<String> values, Map<String, Integer> headerIndex, String columnName, int lineNumber) {
        Integer index = headerIndex.get(columnName);
        if (index == null || index >= values.size()) {
            throw new IllegalArgumentException("Missing value for '" + columnName + "' at CSV line " + lineNumber + ".");
        }

        String value = normalizeToken(values.get(index));
        if (value.isBlank()) {
            throw new IllegalArgumentException("Blank value for '" + columnName + "' at CSV line " + lineNumber + ".");
        }
        return value;
    }

    private LocalDateTime parseTimestamp(String value, int lineNumber) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value, SPACE_SEPARATED_TIMESTAMP);
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("Invalid timestamp format at CSV line " + lineNumber +
                        ". Use ISO format like 2025-01-01T09:30:00.");
            }
        }
    }

    private BigDecimal parsePositiveDecimal(String value, String column, int lineNumber) {
        try {
            BigDecimal number = new BigDecimal(value);
            if (number.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Expected positive '" + column + "' at CSV line " + lineNumber + ".");
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid decimal for '" + column + "' at CSV line " + lineNumber + ".");
        }
    }

    private long parseNonNegativeLong(String value, String column, int lineNumber) {
        try {
            long number = Long.parseLong(value);
            if (number < 0) {
                throw new IllegalArgumentException("Expected non-negative '" + column + "' at CSV line " + lineNumber + ".");
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid integer for '" + column + "' at CSV line " + lineNumber + ".");
        }
    }

    private String findNextNonEmptyLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                return line;
            }
        }
        return null;
    }

    private List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    private String normalizeToken(String token) {
        String trimmed = token.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
