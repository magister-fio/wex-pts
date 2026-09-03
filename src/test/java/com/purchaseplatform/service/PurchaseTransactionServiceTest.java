package com.purchaseplatform.service;

import com.purchaseplatform.domain.PurchaseTransaction;
import com.purchaseplatform.dto.CreatePurchaseTransactionRequest;
import com.purchaseplatform.integration.treasury.TreasuryExchangeRate;
import com.purchaseplatform.integration.treasury.TreasuryExchangeRateClient;
import com.purchaseplatform.repository.PurchaseTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PurchaseTransactionServiceTest {

        private PurchaseTransactionRepository repository;
        private TreasuryExchangeRateClient treasuryClient;
        private PurchaseTransactionService service;

        @BeforeEach
        void setUp() {
                repository = Mockito.mock(PurchaseTransactionRepository.class);
                treasuryClient = Mockito.mock(TreasuryExchangeRateClient.class);

                service = new PurchaseTransactionService(repository, treasuryClient);
        }

        @Test
        void shouldCreatePurchaseTransaction() {
                CreatePurchaseTransactionRequest request = new CreatePurchaseTransactionRequest(
                                "Office supplies",
                                LocalDate.of(2026, 9, 3),
                                new BigDecimal("125.50"));

                when(repository.save(any(PurchaseTransaction.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                PurchaseTransaction result = service.create(request);

                assertNotNull(result.getId());
                assertEquals("Office supplies", result.getDescription());
                assertEquals(LocalDate.of(2026, 9, 3), result.getTransactionDate());
                assertEquals(new BigDecimal("125.50"), result.getPurchaseAmount());

                verify(repository, times(1))
                                .save(any(PurchaseTransaction.class));
        }

        @Test
        void shouldConvertPurchaseAmount() {
                UUID id = UUID.randomUUID();

                PurchaseTransaction transaction = new PurchaseTransaction(
                                id,
                                "Laptop",
                                LocalDate.of(2026, 6, 30),
                                new BigDecimal("100.00"));

                TreasuryExchangeRate rate = new TreasuryExchangeRate(
                                "Canada",
                                "Dollar",
                                new BigDecimal("1.350"),
                                LocalDate.of(2026, 6, 30),
                                LocalDate.of(2026, 6, 30));

                when(repository.findById(id))
                                .thenReturn(Optional.of(transaction));

                when(treasuryClient.findRate("Canada", transaction.getTransactionDate()))
                                .thenReturn(rate);

                var result = service.getConvertedPurchase(id, "Canada");

                assertEquals(new BigDecimal("135.00"), result.convertedAmount());
                assertEquals(new BigDecimal("1.350"), result.exchangeRate());
                assertEquals("Dollar", result.currency());
        }

        @Test
        void shouldThrowExceptionWhenPurchaseNotFound() {
                UUID id = UUID.randomUUID();

                when(repository.findById(id))
                                .thenReturn(Optional.empty());

                assertThrows(
                                com.purchaseplatform.exception.PurchaseNotFoundException.class,
                                () -> service.getConvertedPurchase(id, "Canada"));
        }

        @Test
        void shouldRoundConvertedAmountToTwoDecimalPlaces() {
                UUID id = UUID.randomUUID();

                PurchaseTransaction transaction = new PurchaseTransaction(
                                id,
                                "Test purchase",
                                LocalDate.of(2026, 6, 30),
                                new BigDecimal("100.00"));

                TreasuryExchangeRate rate = new TreasuryExchangeRate(
                                "Canada",
                                "Dollar",
                                new BigDecimal("1.23456"),
                                LocalDate.of(2026, 6, 30),
                                LocalDate.of(2026, 6, 30));

                when(repository.findById(id))
                                .thenReturn(Optional.of(transaction));

                when(treasuryClient.findRate("Canada", transaction.getTransactionDate()))
                                .thenReturn(rate);

                var result = service.getConvertedPurchase(id, "Canada");

                assertEquals(
                                new BigDecimal("123.46"),
                                result.convertedAmount());
        }
}