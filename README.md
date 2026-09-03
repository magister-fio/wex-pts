# Purchase Transaction Service

Spring Boot REST API for storing USD purchase transactions and retrieving them converted into a target country's currency using U.S. Treasury exchange rates.

## Tech Stack

- Java 26
- Spring Boot 4.1.1
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- Testcontainers
- JUnit 5
- Mockito
- Swagger / OpenAPI
- Docker
- GitHub Actions

## Requirements Implemented

### Store Purchase Transaction

Stores:

- Unique ID
- Description
- Transaction date
- Purchase amount in USD

Validation:

- Description maximum 50 characters
- Valid transaction date
- Positive purchase amount
- Maximum 2 decimal places

### Retrieve Converted Purchase

Retrieves a stored purchase and converts the USD amount using the Treasury Reporting Rates of Exchange API.

The exchange rate must:

- Be dated on or before the purchase date
- Be no more than 6 months older than the purchase date
- Use the newest applicable rate
- Return an error if no valid rate exists

Converted amounts are rounded to 2 decimal places.

## Architecture

```text
Client / Swagger
       |
       v
REST Controller
       |
       v
Service Layer
       |
       +-------------------+
       |                   |
       v                   v
JPA Repository      Treasury API Client
       |                   |
       v                   v
PostgreSQL          U.S. Treasury API