package com.tradingsim.infrastructure.csv;

import com.tradingsim.domain.Candle;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvCandleServiceTest {

    private final CsvCandleService service = new CsvCandleService();

    @Test
    void parseCandles_validCsv_returnsCandles() {
        MockMultipartFile file = csv("""
                timestamp,open,high,low,close,volume
                2025-01-01T09:30:00,100,101,99,100.5,1000
                2025-01-02T09:30:00,100.5,102,100,101.7,1200
                2025-01-03T09:30:00,101.7,103,101,102.4,1500
                """);

        List<Candle> candles = service.parseCandles(file);

        assertThat(candles).hasSize(3);
        assertThat(candles.get(0).close().toPlainString()).isEqualTo("100.5");
        assertThat(candles.get(2).volume()).isEqualTo(1500);
    }

    @Test
    void parseCandles_outOfOrderTimestamp_throwsHelpfulError() {
        MockMultipartFile file = csv("""
                timestamp,open,high,low,close,volume
                2025-01-02T09:30:00,100,101,99,100.5,1000
                2025-01-01T09:30:00,100.5,102,100,101.7,1200
                """);

        assertThatThrownBy(() -> service.parseCandles(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictly increasing");
    }

    @Test
    void parseCandles_blankValue_throwsHelpfulError() {
        MockMultipartFile file = csv("""
                timestamp,open,high,low,close,volume
                2025-01-01T09:30:00,100,101,99,,1000
                """);

        assertThatThrownBy(() -> service.parseCandles(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blank value for 'close'");
    }

    @Test
    void preview_returnsBasicStatistics() {
        MockMultipartFile file = csv("""
                timestamp,open,high,low,close,volume
                2025-01-01T09:30:00,100,101,99,100.5,1000
                2025-01-02T09:30:00,100.5,102,100,101.7,1200
                2025-01-03T09:30:00,101.7,103,98,98.4,1500
                """);

        CsvPreviewSummary preview = service.preview(file);

        assertThat(preview.candleCount()).isEqualTo(3);
        assertThat(preview.startTimestamp().toString()).isEqualTo("2025-01-01T09:30");
        assertThat(preview.endTimestamp().toString()).isEqualTo("2025-01-03T09:30");
        assertThat(preview.minClose().toPlainString()).isEqualTo("98.4");
        assertThat(preview.maxClose().toPlainString()).isEqualTo("101.7");
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile(
                "file",
                "candles.csv",
                "text/csv",
                content.getBytes()
        );
    }
}
