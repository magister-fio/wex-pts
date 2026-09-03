package com.purchaseplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_transaction")
public class PurchaseTransaction {

    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(
        name = "purchase_amount",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal purchaseAmount;

    protected PurchaseTransaction() {
    }

    public PurchaseTransaction(
            UUID id,
            String description,
            LocalDate transactionDate,
            BigDecimal purchaseAmount
    ) {
        this.id = id;
        this.description = description;
        this.transactionDate = transactionDate;
        this.purchaseAmount = purchaseAmount;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public BigDecimal getPurchaseAmount() {
        return purchaseAmount;
    }
}