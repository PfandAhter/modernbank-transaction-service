# ModernBank Transaction Service

## Overview
The ModernBank Transaction Service is a Spring Boot 3 microservice that manages customer transaction flows for the ModernBank platform. It coordinates account debits and credits, orchestrates ATM operations, and publishes transactional events so that other bounded contexts (notifications, reporting, parameter services, etc.) can react to changes.

The service exposes REST endpoints for initiating transfers, withdrawals, and deposits. It integrates with downstream services through OpenFeign, publishes domain events to Apache Kafka, and caches frequently accessed information with Redis. Persistence is handled via MySQL using Spring Data JPA.

## Features
- **RESTful transaction endpoints** for transferring funds, ATM withdrawals, and fetching transaction history.
- **Event-driven architecture** using Kafka topics to broadcast state changes across the platform.
- **Service-to-service communication** powered by Spring Cloud OpenFeign clients for account, notification, parameter, and ATM services.
- **Caching layer** backed by Redis to accelerate lookup-heavy operations.
- **Scheduled and asynchronous processing hooks** enabled through Spring's scheduling and Kafka support for background workflows.

## Tech Stack
- Java 17
- Spring Boot 3.2
- Spring Data JPA & MySQL
- Spring Cloud OpenFeign
- Apache Kafka
- Redis Cache
- Maven Wrapper (`mvnw`)

## Getting Started
### Prerequisites
- Java Development Kit (JDK) 17
- Docker or local installations for the following infrastructure services:
  - MySQL (default URL: `jdbc:mysql://localhost:3306/modern_bank_transaction_service`)
  - Apache Kafka (default bootstrap server: `localhost:9092`)
  - Redis (default host: `localhost`, port `6379`)
- Optional: Docker Compose to spin up dependencies quickly

### Configuration
The default configuration values are defined in [`src/main/resources/application.yml`](src/main/resources/application.yml). Update the datasource, Kafka, and Feign client URLs as needed for your environment before running the application.

### Build & Run
Use the Maven Wrapper to build and run the service:

```bash
./mvnw clean package
./mvnw spring-boot:run
```

If you prefer a local Maven installation, replace `./mvnw` with `mvn`.

### Running Tests
Execute the unit test suite with:

```bash
./mvnw test
```

> **Note:** The Maven Wrapper downloads dependencies and a compatible Maven runtime on first use. Ensure the environment has outbound internet access to Maven Central.

## API Endpoints
All routes are rooted at `/api/v1/transaction`:

| Method | Path | Description |
| ------ | ---- | ----------- |
| POST | `/withdraw` | Initiate an account withdrawal. |
| POST | `/deposit` | Deposit funds into an account. |
| POST | `/transfer` | Transfer money between accounts. |
| POST | `/transfer/atm` | Transfer funds via an ATM workflow. |
| POST | `/withdraw/atm` | Withdraw funds from an ATM. |
| GET | `/transactions` | Retrieve paginated transactions for an account (legacy). |
| POST | `/transactionsv2` | Retrieve paginated transactions via request payload. |

Request and response payloads are defined under [`src/main/java/com/modernbank/transaction_service/api`](src/main/java/com/modernbank/transaction_service/api).

## Development Tips
- Enable required infrastructure services (MySQL, Kafka, Redis) locally or via containers before exercising the endpoints.
- Kafka topic names, Feign client routes, and caching options are all customizable through `application.yml`.
- Use the `MapperService` utilities to convert between entities, models, and DTOs when extending the service.

## License
This project does not currently declare a license. Add one before distributing the code.
