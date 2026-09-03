package com.purchaseplatform.integration.treasury;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.purchaseplatform.exception.ExchangeRateNotFoundException;

import java.time.LocalDate;

@Component
public class TreasuryExchangeRateClient {

        private static final String BASE_URL = "https://api.fiscaldata.treasury.gov/services/api/fiscal_service";

        private final RestClient restClient;

        public TreasuryExchangeRateClient() {
                this(RestClient.builder());
        }

        TreasuryExchangeRateClient(RestClient.Builder builder) {
                this.restClient = builder
                                .baseUrl(BASE_URL)
                                .build();
        }

        public TreasuryExchangeRate findRate(
                        String country,
                        LocalDate purchaseDate) {
                LocalDate earliestDate = purchaseDate.minusMonths(6);

                TreasuryExchangeRateResponse response = restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/v1/accounting/od/rates_of_exchange")
                                                .queryParam(
                                                                "fields",
                                                                "country,currency,exchange_rate,record_date,effective_date")
                                                .queryParam(
                                                                "filter",
                                                                "country:eq:" + country
                                                                                + ",record_date:gte:" + earliestDate
                                                                                + ",record_date:lte:" + purchaseDate)
                                                .queryParam("sort", "-record_date")
                                                .queryParam("page[size]", 1)
                                                .build())
                                .retrieve()
                                .body(TreasuryExchangeRateResponse.class);

                if (response == null
                                || response.data() == null
                                || response.data().isEmpty()) {
                        throw new ExchangeRateNotFoundException(
                                        "No exchange rate is available within 6 months of the purchase date");
                }

                return response.data().getFirst();
        }
}