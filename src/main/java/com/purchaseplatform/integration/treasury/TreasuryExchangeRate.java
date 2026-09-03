package com.purchaseplatform.integration.treasury;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TreasuryExchangeRate(

        String country,

        String currency,

        @JsonProperty("exchange_rate")
        BigDecimal exchangeRate,

        @JsonProperty("record_date")
        LocalDate recordDate,

        @JsonProperty("effective_date")
        LocalDate effectiveDate

) {
}