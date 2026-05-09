package com.tradingsim.infrastructure.marketdata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlphaVantageMarketDataProviderTest {

    private final AlphaVantageMarketDataProvider provider = new AlphaVantageMarketDataProvider("test-key");

    @Test
    void deriveProviderErrorMessage_readsInformationField() {
        String body = """
                {"Information":"The demo API key is for demo purposes only. Please claim your free API key."}
                """;

        String message = provider.deriveProviderErrorMessage(body);

        assertThat(message).contains("Alpha Vantage response:");
        assertThat(message).contains("demo API key");
    }

    @Test
    void deriveProviderErrorMessage_readsNoteField() {
        String body = """
                {"Note":"Thank you for using Alpha Vantage! Our standard API call frequency is 25 requests per day."}
                """;

        String message = provider.deriveProviderErrorMessage(body);

        assertThat(message).contains("Alpha Vantage response:");
        assertThat(message).contains("25 requests per day");
    }

    @Test
    void deriveProviderErrorMessage_fallsBackForUnknownJson() {
        String body = """
                {"foo":"bar"}
                """;

        String message = provider.deriveProviderErrorMessage(body);

        assertThat(message).isEqualTo("Unexpected response from Alpha Vantage. Check API key, symbol, and rate limits.");
    }
}
