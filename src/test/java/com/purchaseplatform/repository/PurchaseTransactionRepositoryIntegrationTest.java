package com.purchaseplatform.repository;

import com.purchaseplatform.domain.PurchaseTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class PurchaseTransactionRepositoryIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16")
                    .withDatabaseName("wex_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PurchaseTransactionRepository repository;

    @Test
    void shouldPersistAndRetrievePurchaseTransaction() {
        UUID id = UUID.randomUUID();

        PurchaseTransaction transaction =
                new PurchaseTransaction(
                        id,
                        "Integration test",
                        LocalDate.of(2026, 9, 3),
                        new BigDecimal("199.99")
                );

        repository.save(transaction);

        PurchaseTransaction saved = repository.findById(id)
                .orElseThrow();

        assertEquals(id, saved.getId());
        assertEquals("Integration test", saved.getDescription());
        assertEquals(
                new BigDecimal("199.99"),
                saved.getPurchaseAmount()
        );
    }
}