# Engineering Handoff — Purchase Transaction Service

**Project:** WEX Purchase Transaction Service  
**Repository:** `wex-pts`  
**Status:** Ready for review / handoff  
**Primary branch:** `main`

---

## 1. Current State

The service is functionally complete for the current assignment scope.

Implemented:

- Store a USD purchase transaction
- Generate a unique UUID for each purchase
- Validate description, date, and purchase amount
- Persist purchases in PostgreSQL
- Retrieve a stored purchase
- Query the U.S. Treasury exchange-rate API
- Apply the six-month exchange-rate rule
- Convert the USD purchase amount to the target currency
- Round converted amounts to two decimal places
- Return structured API errors
- Expose Swagger / OpenAPI documentation
- Run automated unit, controller, client, and database integration tests
- Run CI with Maven `verify`
- Build a deployable JAR
- Upload the JAR from GitHub Actions

---

## 2. Main Architecture

The service uses a simple layered Spring Boot design.

```text
Client / Swagger
       |
       v
REST Controller
       |
       v
Request Validation
       |
       v
PurchaseTransactionService
       |
       +--------------------------+
       |                          |
       v                          v
PurchaseTransactionRepository    TreasuryExchangeRateClient
       |                          |
       v                          v
PostgreSQL                 U.S. Treasury API
```

Flyway is separate from the normal request path:

```text
Flyway Migrations
       |
       v
PostgreSQL
```

Flyway manages schema creation and upgrades during application startup. Repository reads and writes go directly through JPA to PostgreSQL.

---

## 3. Key Engineering Decisions

### 3.1 PostgreSQL for Persistence

**Decision:** Use PostgreSQL as the transaction store.

**Why:**

- Purchase transactions are structured relational data.
- The application needs reliable persistence and lookup by unique ID.
- PostgreSQL works well with Spring Data JPA.
- It is easy to run locally through Docker and in integration tests through Testcontainers.

### 3.2 Flyway Owns Database Schema Changes

**Decision:** Use Flyway migrations instead of Hibernate automatic schema updates.

Hibernate is configured with schema validation rather than schema mutation.

**Why:**

- Schema changes are explicit and version controlled.
- The same migration is used locally, in CI, and in Testcontainers.
- A fresh database can be created consistently.
- Database changes are easier to review.

**Important:** Do not switch Hibernate to automatic schema updates unless the migration strategy is intentionally changed.

### 3.3 `BigDecimal` for Money

**Decision:** Use `BigDecimal` for purchase amounts and converted amounts.

**Why:** Money should not use floating-point types such as `double` because they can introduce precision errors. The database column also uses an exact numeric type.

### 3.4 DTO Validation at the API Boundary

**Decision:** Validate request data in the request DTO and trigger it from the controller.

Examples of rules:

- Description: required and maximum 50 characters
- Transaction date: required and must parse as a real date
- Purchase amount: positive
- Purchase amount: maximum two decimal places

**Why:** Invalid requests should be rejected before they reach business logic. The service layer stays focused on business behavior rather than HTTP input validation.

### 3.5 Layered Service Structure

**Decision:** Keep controller, service, repository, and Treasury API integration separate.

**Why:**

- Controller handles HTTP concerns.
- Service handles business logic.
- Repository handles persistence.
- Treasury client handles external API communication.

This makes the code easier to test and keeps external API logic out of the main service.

### 3.6 Exchange-Rate Selection Rule

**Decision:** For a purchase date, search only for Treasury rates that are on or before the purchase date and no more than six months older than the purchase date. If more than one valid rate exists, use the most recent one.

**Why:** This directly implements the assignment’s currency-conversion rule. The client also validates the returned record before accepting it.

### 3.7 Testcontainers for PostgreSQL Integration Testing

**Decision:** Use Testcontainers instead of requiring a developer’s local PostgreSQL for integration tests.

**Why:**

- Tests get a clean PostgreSQL instance.
- Flyway is tested against a fresh database.
- CI does not depend on local machine setup.
- Tests are more repeatable.

### 3.8 Maven `verify` in CI

**Decision:** CI runs `./mvnw clean verify` instead of only `test`.

**Why:** `verify` runs the tests and also confirms that the deployable JAR can be packaged successfully. The resulting JAR is uploaded as a GitHub Actions artifact.

---

## 4. Short Code Review

### `PurchaseTransactionController`

**Role:** HTTP entry point.

Responsibilities:

- Accept POST requests for new purchases
- Accept GET requests for converted purchases
- Trigger request validation
- Return API response DTOs

**Review note:** Controller is intentionally thin. Business logic remains in the service.

### `CreatePurchaseTransactionRequest`

**Role:** Defines the incoming API contract.

Responsibilities:

- Holds description, transaction date, and amount
- Contains validation rules

**Review note:** Good place for request validation because invalid data is rejected before business logic runs.

### `PurchaseTransactionService`

**Role:** Main business-logic layer.

Responsibilities:

- Generate purchase UUID
- Save purchases
- Retrieve purchases
- Request Treasury exchange rates
- Calculate converted amount
- Round converted amount to two decimal places

**Review note:** This is the central business class. It should remain free of HTTP-specific behavior.

### `PurchaseTransactionRepository`

**Role:** Persistence abstraction.

Responsibilities:

- Save transactions
- Retrieve transactions by UUID

**Review note:** Spring Data JPA keeps this layer intentionally small.

### `PurchaseTransaction`

**Role:** JPA entity mapped to PostgreSQL.

Responsibilities:

- Represent stored purchase data
- Map Java fields to database columns

**Review note:** `BigDecimal` is used for monetary data and the database schema applies matching precision.

### `TreasuryExchangeRateClient`

**Role:** External API adapter.

Responsibilities:

- Build the Treasury query
- Apply the allowed date window
- Request the newest valid rate
- Parse Treasury responses
- Reject empty or invalid rate results

**Review note:** Keeping this logic outside the service makes Treasury integration easier to test independently.

### `GlobalExceptionHandler`

**Role:** Convert application exceptions into consistent HTTP responses.

Main cases:

- `400` — invalid request data
- `404` — purchase not found
- `422` — purchase exists but cannot be converted because no valid rate is available

**Review note:** This prevents expected business failures from becoming generic `500` responses.

### Flyway Migration

**Role:** Create and version the PostgreSQL schema.

Current migration:

```text
src/main/resources/db/migration/V1__create_purchase_transaction_table.sql
```

**Review note:** Flyway manages schema lifecycle; it is not part of normal repository reads/writes.

---

## 5. Bugs Encountered and Fixes

Only actual implementation/runtime/test bugs are recorded here.

### Bug 1 — Hibernate Reported Missing Table

**Symptom**

Application startup failed with:

```text
Schema validation: missing table [purchase_transaction]
```

**Investigation**

The logs showed that PostgreSQL connection setup was successful, so the problem was not connectivity. Hibernate was validating the schema, but the Flyway migration had not created the table.

**Root Cause**

Flyway was not being auto-configured correctly for the Spring Boot 4 project.

**Fix**

Used the proper Spring Boot Flyway starter together with the PostgreSQL Flyway module.

After the change:

1. Spring connected to PostgreSQL.
2. Flyway ran the migration.
3. The table was created.
4. Hibernate schema validation passed.

**Takeaway:** When startup fails, identify which initialization stage succeeded before changing unrelated configuration.

### Bug 2 — `RestClient.Builder` Bean Was Missing

**Symptom**

Application startup failed because Spring could not create `TreasuryExchangeRateClient`. The error reported that no qualifying `RestClient.Builder` bean was available.

**Root Cause**

The Treasury client constructor expected Spring to inject a builder that was not registered as a bean in the current setup.

**Fix**

The client now creates its production `RestClient` directly. A secondary constructor accepts a builder for tests.

**Takeaway:** External clients should be easy to construct in production while still allowing dependencies to be replaced during testing.

### Bug 3 — Treasury Result Needed Defensive Validation

**Symptom**

During manual testing, a conversion response returned a currency that did not match the intended country test.

**Root Cause**

The application accepted the first returned record without independently confirming that it matched the requested country and allowed date window.

**Fix**

Added validation before accepting the returned rate:

- returned country must match the requested country
- rate date must not be before the six-month boundary
- rate date must not be after the purchase date

If the record does not satisfy the rule, the service treats it as no valid exchange rate.

**Takeaway:** Business rules should still be validated inside the application even when an upstream API is expected to enforce equivalent filters.

### Bug 4 — Duplicate / Misplaced Service Test

**Symptom**

Maven reported one passing service test and another failing test with a class-loading error.

**Root Cause**

A duplicate test existed under a different package after the test file had been moved.

**Fix**

Removed the stale duplicate and kept the correctly packaged test under:

```text
src/test/java/com/purchaseplatform/service/
```

**Takeaway:** After moving Java classes, check for stale copies because Maven will discover every matching test class.

### Bug 5 — MVC Test Support Was Missing

**Symptom**

Controller tests failed to compile because `@WebMvcTest` could not be resolved.

**Root Cause**

Spring Boot 4 separates MVC test support into its dedicated test starter.

**Fix**

Added the Spring Boot MVC test dependency. Controller tests then compiled and ran successfully.

**Takeaway:** Major framework versions can reorganize dependency boundaries even when the testing concepts remain the same.

### Bug 6 — Treasury Client Test Matcher Did Not Compile

**Symptom**

The mocked HTTP test failed to compile when a lambda was passed to the request matcher.

**Root Cause**

The Spring test API expected a Hamcrest string matcher rather than the lambda form used initially.

**Fix**

Changed the request expectation to a string matcher that checks whether the Treasury endpoint path is present.

**Takeaway:** Test helper APIs can change between framework versions; keep test code aligned with the exact Spring version in the project.

### Bug 7 — GitHub CI Failed While Local Tests Passed

**Symptom**

Local tests passed, but GitHub Actions failed while loading the application context.

**Root Cause**

The generated Spring context test expected PostgreSQL to exist at the local development URL. That worked on the developer machine because PostgreSQL was running locally, but it was not available on the GitHub runner.

**Fix**

Removed the redundant generated context test. The Testcontainers integration test already verifies:

- Spring application context
- PostgreSQL
- Flyway
- JPA
- repository persistence

**Takeaway:** CI tests should not depend on developer-machine infrastructure.

---

## 6. Testing State

Automated coverage includes:

- Service unit tests
- Controller/API tests
- Request validation tests
- Treasury API client tests
- Error-response tests
- PostgreSQL repository integration test
- Flyway migration verification through Testcontainers

The Treasury client tests use a mocked HTTP server and do not depend on the real Treasury service.

The PostgreSQL integration test uses a temporary real PostgreSQL container.

---

## 7. Build and CI

Local full verification:

```text
./mvnw clean verify
```

GitHub Actions performs the same build.

CI verifies:

- compilation
- automated tests
- Testcontainers database integration
- JAR packaging

The deployable JAR is uploaded as a workflow artifact.

---

## 8. Operational Notes

### Start PostgreSQL

```text
docker compose up -d
```

### Start Application

```text
./mvnw spring-boot:run
```

Windows:

```text
.\mvnw.cmd spring-boot:run
```

### Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

### Run Tests

```text
./mvnw clean test
```

### Full Build

```text
./mvnw clean verify
```

---

## 9. Important Files

```text
src/main/java/com/purchaseplatform/
```
Main application code.

```text
src/main/resources/application.properties
```
Runtime configuration.

```text
src/main/resources/db/migration/
```
Flyway migrations.

```text
src/test/java/com/purchaseplatform/
```
Automated tests.

```text
.github/workflows/ci.yml
```
CI pipeline.

```text
docs/architecture/
```
System design documentation.

---

## 10. Known Limitations

For the current assignment scope:

- Treasury API availability is an external dependency.
- Country input depends on Treasury naming.
- Exchange rates are fetched at request time.
- There is no exchange-rate cache.
- There is no retry/circuit-breaker policy.
- Authentication is not implemented.
- Production metrics/monitoring are not configured.

These are not blockers for the current requirements.

---

## 11. Reasonable Next Improvements

If the project continues:

1. Add caching for Treasury rates.
2. Add retry / circuit-breaker behavior for Treasury failures.
3. Add structured logging and correlation IDs.
4. Add health and metrics endpoints to deployment monitoring.
5. Add deployment/container-image configuration.
6. Add authentication if the API becomes externally exposed.

---

## 12. Final Handoff Summary

The current implementation is ready for review.

Before making changes, future maintainers should understand these areas first:

1. Flyway owns the database schema.
2. Monetary values use exact decimal types.
3. Treasury exchange-rate selection is constrained by the six-month rule.
4. The external Treasury client is isolated from core business logic.
5. Database integration tests use Testcontainers so CI does not depend on local PostgreSQL.
6. CI uses Maven `verify` to test and package the deployable JAR.
