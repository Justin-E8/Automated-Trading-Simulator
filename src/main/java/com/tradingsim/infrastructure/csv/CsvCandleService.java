package com.tradingsim.infrastructure.csv;

import com.tradingsim.application.CandleValidationService;
import com.tradingsim.domain.Candle;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CSV ingestion and normalization for historical candles.
 *
 * <p>Supports both simulator-native headers and Yahoo Finance export headers,
 * then normalizes rows into domain {@link Candle} objects with shared validation.</p>
 */
@Service
public class CsvCandleService {

    private static final List<String> REQUIRED_COLUMNS = List.of("timestamp", "open", "high", "low", "close", "volume");
    private static final DateTimeFormatter SPACE_SEPARATED_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, List<String>> COLUMN_ALIASES = Map.of(
            "timestamp", List.of("timestamp", "date"),
            "open", List.of("open"),
            "high", List.of("high"),
            "low", List.of("low"),
            "close", List.of("close", "adj close"),
            "volume", List.of("volume")
    );
    private final CandleValidationService candleValidationService;

    public CsvCandleService(CandleValidationService candleValidationService) {
        this.candleValidationService = candleValidationService;
    }

    /**
     * Parses and validates all candles from an uploaded CSV file.
     */
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
            Map<String, Integer> resolvedColumns = resolveCanonicalColumns(headerIndex);

            List<Candle> candles = new ArrayList<>();
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> values = splitCsvLine(line);
                Candle candle = parseCandleRow(values, resolvedColumns, lineNumber);
                candles.add(candle);
            }

            if (candles.isEmpty()) {
                throw new IllegalArgumentException("CSV contains no candle rows.");
            }

            candleValidationService.validateChronologicalOrder(candles, "CSV file");
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

    private Map<String, Integer> resolveCanonicalColumns(Map<String, Integer> headerIndex) {
        Map<String, Integer> resolved = new HashMap<>();
        List<String> missing = REQUIRED_COLUMNS.stream()
                .filter(column -> resolveColumnIndex(column, headerIndex) == null)
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("CSV is missing required columns: " + String.join(", ", missing)
                    + ". Supported aliases include Yahoo headers like Date and Adj Close.");
        }
        for (String required : REQUIRED_COLUMNS) {
            resolved.put(required, resolveColumnIndex(required, headerIndex));
        }
        return resolved;
    }

    private Integer resolveColumnIndex(String canonicalColumn, Map<String, Integer> headerIndex) {
        List<String> aliases = COLUMN_ALIASES.getOrDefault(canonicalColumn, List.of(canonicalColumn));
        for (String alias : aliases) {
            Integer idx = headerIndex.get(alias.toLowerCase(Locale.ROOT));
            if (idx != null) {
                return idx;
            }
        }
        return null;
    }

    private Candle parseCandleRow(List<String> values, Map<String, Integer> resolvedColumns, int lineNumber) {
        LocalDateTime timestamp = parseTimestamp(requiredValue(values, resolvedColumns.get("timestamp"), "timestamp", lineNumber), lineNumber);
        BigDecimal open = parsePositiveDecimal(requiredValue(values, resolvedColumns.get("open"), "open", lineNumber), "open", lineNumber);
        BigDecimal high = parsePositiveDecimal(requiredValue(values, resolvedColumns.get("high"), "high", lineNumber), "high", lineNumber);
        BigDecimal low = parsePositiveDecimal(requiredValue(values, resolvedColumns.get("low"), "low", lineNumber), "low", lineNumber);
        BigDecimal close = parsePositiveDecimal(requiredValue(values, resolvedColumns.get("close"), "close", lineNumber), "close", lineNumber);
        long volume = parseNonNegativeLong(requiredValue(values, resolvedColumns.get("volume"), "volume", lineNumber), "volume", lineNumber);

        Candle candle = new Candle(timestamp, open, high, low, close, volume);
        candleValidationService.validateCandle(candle, "CSV line " + lineNumber);
        return candle;
    }

    private String requiredValue(List<String> values, Integer index, String columnName, int lineNumber) {
        if (index == null || index >= values.size()) {
            throw new IllegalArgumentException("Missing value for '" + columnName + "' at CSV line " + lineNumber + ".");
        }

        String value = normalizeToken(values.get(index));
        if (value.isBlank() || "null".equalsIgnoreCase(value) || "n/a".equalsIgnoreCase(value)) {
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
                try {
                    return LocalDate.parse(value).atTime(9, 30);
                } catch (DateTimeParseException dateParseException) {
                    throw new IllegalArgumentException("Invalid timestamp format at CSV line " + lineNumber +
                            ". Use ISO datetime (2025-01-01T09:30:00) or date-only format (2025-01-01).");
                }
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
