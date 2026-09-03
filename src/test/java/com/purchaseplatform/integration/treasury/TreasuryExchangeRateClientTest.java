package com.purchaseplatform.integration.treasury;

import com.purchaseplatform.exception.ExchangeRateNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class TreasuryExchangeRateClientTest {

    private TreasuryExchangeRateClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        client = new TreasuryExchangeRateClient(builder);
    }

    @Test
    void shouldReturnExchangeRate() {
        server.expect(requestTo(
                containsString("/v1/accounting/od/rates_of_exchange")))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {
                              "country": "Canada",
                              "currency": "Dollar",
                              "exchange_rate": "1.350",
                              "record_date": "2026-06-30",
                              "effective_date": "2026-06-30"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        TreasuryExchangeRate rate = client.findRate(
                "Canada",
                LocalDate.of(2026, 7, 15));

        assertEquals("Canada", rate.country());
        assertEquals("Dollar", rate.currency());
        assertEquals("1.350", rate.exchangeRate().toPlainString());

        server.verify();
    }

    @Test
    void shouldThrowWhenNoRateExists() {
        server.expect(requestTo(
                containsString("/v1/accounting/od/rates_of_exchange")))
                .andRespond(withSuccess("""
                        {
                          "data": []
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThrows(
                ExchangeRateNotFoundException.class,
                () -> client.findRate(
                        "Canada",
                        LocalDate.of(1990, 1, 15)));

        server.verify();
    }
}