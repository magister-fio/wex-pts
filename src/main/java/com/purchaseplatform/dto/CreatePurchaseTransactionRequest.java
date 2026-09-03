package com.purchaseplatform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePurchaseTransactionRequest(

        @NotBlank
        @Size(max = 50)
        String description,

        @NotNull
        LocalDate transactionDate,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 8, fraction = 2)
        BigDecimal purchaseAmount

) {
}