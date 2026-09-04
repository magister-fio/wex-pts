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

## How to Set Up

### Prerequisites

Install:

- Java 26
- Docker
- Git

The project includes the Maven Wrapper, so Maven does not need to be installed separately.

### 1. Clone the Repository

```bash
git clone https://github.com/magister-fio/wex-pts.git
cd wex-pts
```

### 2. Start PostgreSQL

Make sure Docker is running, then execute:

```bash
docker compose up -d
```

Verify the PostgreSQL container:

```bash
docker ps
```

### 3. Run the Application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux/macOS:

```bash
./mvnw spring-boot:run
```

The application runs at:

`http://localhost:8080`

### 4. Open Swagger

Open:

`http://localhost:8080/swagger-ui/index.html`

Swagger can be used to test both the POST and GET endpoints.

### 5. Run Automated Tests

Windows:

```powershell
.\mvnw.cmd clean test
```

Linux/macOS:

```bash
./mvnw clean test
```

The test suite includes:

- Service unit tests
- Controller/API tests
- Validation tests
- Treasury API client tests
- PostgreSQL integration tests using Testcontainers
- Flyway migration verification

### 6. Build the Deployable JAR

Windows:

```powershell
.\mvnw.cmd clean verify
```

Linux/macOS:

```bash
./mvnw clean verify
```

The generated JAR is created under:

`target/`

GitHub Actions also runs the build and test suite automatically on pushes and pull requests.

