package com.purchaseplatform.controller;

import com.purchaseplatform.domain.PurchaseTransaction;
import com.purchaseplatform.dto.ConvertedPurchaseTransactionResponse;
import com.purchaseplatform.dto.CreatePurchaseTransactionRequest;
import com.purchaseplatform.dto.PurchaseTransactionResponse;
import com.purchaseplatform.service.PurchaseTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseTransactionController {

    private final PurchaseTransactionService service;

    public PurchaseTransactionController(PurchaseTransactionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseTransactionResponse create(
            @Valid @RequestBody CreatePurchaseTransactionRequest request
    ) {
        PurchaseTransaction transaction = service.create(request);

        return new PurchaseTransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getPurchaseAmount()
        );
    }

    @GetMapping("/{id}")
    public ConvertedPurchaseTransactionResponse getConvertedPurchase(
            @PathVariable UUID id,
            @RequestParam String country
    ) {
        return service.getConvertedPurchase(id, country);
    }
}