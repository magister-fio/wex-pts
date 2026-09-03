package com.purchaseplatform.service;

import com.purchaseplatform.domain.PurchaseTransaction;
import com.purchaseplatform.dto.ConvertedPurchaseTransactionResponse;
import com.purchaseplatform.dto.CreatePurchaseTransactionRequest;
import com.purchaseplatform.exception.PurchaseNotFoundException;
import com.purchaseplatform.integration.treasury.TreasuryExchangeRate;
import com.purchaseplatform.integration.treasury.TreasuryExchangeRateClient;
import com.purchaseplatform.repository.PurchaseTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class PurchaseTransactionService {

        private final PurchaseTransactionRepository repository;
        private final TreasuryExchangeRateClient treasuryClient;

        public PurchaseTransactionService(
                        PurchaseTransactionRepository repository,
                        TreasuryExchangeRateClient treasuryClient) {
                this.repository = repository;
                this.treasuryClient = treasuryClient;
        }

        public PurchaseTransaction create(CreatePurchaseTransactionRequest request) {
                PurchaseTransaction transaction = new PurchaseTransaction(
                                UUID.randomUUID(),
                                request.description(),
                                request.transactionDate(),
                                request.purchaseAmount());

                return repository.save(transaction);
        }

        public ConvertedPurchaseTransactionResponse getConvertedPurchase(
                        UUID id,
                        String country) {
                PurchaseTransaction transaction = repository.findById(id)
                                .orElseThrow(() -> 
                                        new PurchaseNotFoundException("Purchase transaction not found")
                                );

                TreasuryExchangeRate rate = treasuryClient.findRate(
                                country,
                                transaction.getTransactionDate());

                BigDecimal convertedAmount = transaction.getPurchaseAmount()
                                .multiply(rate.exchangeRate())
                                .setScale(2, RoundingMode.HALF_UP);

                return new ConvertedPurchaseTransactionResponse(
                                transaction.getId(),
                                transaction.getDescription(),
                                transaction.getTransactionDate(),
                                transaction.getPurchaseAmount(),
                                rate.currency(),
                                rate.exchangeRate(),
                                convertedAmount);
        }
}