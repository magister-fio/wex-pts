package com.purchaseplatform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ConvertedPurchaseTransactionResponse(
        UUID id,
        String description,
        LocalDate transactionDate,
        BigDecimal purchaseAmountUsd,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal convertedAmount
) {
}