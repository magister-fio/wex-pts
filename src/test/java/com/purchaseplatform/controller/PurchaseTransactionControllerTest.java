package com.purchaseplatform.controller;

import com.purchaseplatform.domain.PurchaseTransaction;
import com.purchaseplatform.dto.ConvertedPurchaseTransactionResponse;
import com.purchaseplatform.service.PurchaseTransactionService;
import com.purchaseplatform.exception.ExchangeRateNotFoundException;
import com.purchaseplatform.exception.PurchaseNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseTransactionController.class)
class PurchaseTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseTransactionService service;

    @Test
    void shouldCreatePurchaseTransaction() throws Exception {
        UUID id = UUID.randomUUID();

        PurchaseTransaction transaction = new PurchaseTransaction(
                id,
                "Office supplies",
                LocalDate.of(2026, 9, 3),
                new BigDecimal("125.50"));

        when(service.create(any()))
                .thenReturn(transaction);

        mockMvc.perform(post("/api/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "description": "Office supplies",
                          "transactionDate": "2026-09-03",
                          "purchaseAmount": 125.50
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.description").value("Office supplies"))
                .andExpect(jsonPath("$.transactionDate").value("2026-09-03"))
                .andExpect(jsonPath("$.purchaseAmount").value(125.50));
    }

    @Test
    void shouldRejectInvalidPurchaseAmount() throws Exception {
        mockMvc.perform(post("/api/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "description": "Office supplies",
                          "transactionDate": "2026-09-03",
                          "purchaseAmount": -10.00
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConvertedPurchase() throws Exception {
        UUID id = UUID.randomUUID();

        ConvertedPurchaseTransactionResponse response = new ConvertedPurchaseTransactionResponse(
                id,
                "Office supplies",
                LocalDate.of(2026, 9, 3),
                new BigDecimal("100.00"),
                "Dollar",
                new BigDecimal("1.350"),
                new BigDecimal("135.00"));

        when(service.getConvertedPurchase(id, "Canada"))
                .thenReturn(response);

        mockMvc.perform(get("/api/purchases/{id}", id)
                .param("country", "Canada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.currency").value("Dollar"))
                .andExpect(jsonPath("$.exchangeRate").value(1.350))
                .andExpect(jsonPath("$.convertedAmount").value(135.00));
    }

    @Test
    void shouldRejectDescriptionLongerThan50Characters() throws Exception {
        mockMvc.perform(post("/api/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "description": "This description is intentionally longer than fifty characters for testing",
                          "transactionDate": "2026-09-03",
                          "purchaseAmount": 100.00
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingTransactionDate() throws Exception {
        mockMvc.perform(post("/api/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "description": "Office supplies",
                          "purchaseAmount": 100.00
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMoreThanTwoDecimalPlaces() throws Exception {
        mockMvc.perform(post("/api/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "description": "Office supplies",
                          "transactionDate": "2026-09-03",
                          "purchaseAmount": 100.123
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenPurchaseNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.getConvertedPurchase(id, "Canada"))
                .thenThrow(new PurchaseNotFoundException(
                        "Purchase transaction not found"));

        mockMvc.perform(get("/api/purchases/{id}", id)
                .param("country", "Canada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Purchase transaction not found"));
    }

    @Test
    void shouldReturn422WhenExchangeRateNotAvailable() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.getConvertedPurchase(id, "Canada"))
                .thenThrow(new ExchangeRateNotFoundException(
                        "No exchange rate is available within 6 months of the purchase date"));

        mockMvc.perform(get("/api/purchases/{id}", id)
                .param("country", "Canada"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message")
                        .value("No exchange rate is available within 6 months of the purchase date"));
    }
}