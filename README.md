# Task Management API

Backend application for the HMCTS developer coding challenge, built with Java, Spring Boot, PostgreSQL and Gradle.

## Technology Stack

- Java 21
- Spring Boot
- Gradle
- PostgreSQL 17
- Spring Data JPA / Hibernate
- Flyway for database migrations
- JUnit and AssertJ for testing
- Docker Compose for local database setup

## Prerequisites

- Java 21
- Docker and Docker Compose
- A working Gradle wrapper (included in the repository)

## Local Setup

### 1. Start PostgreSQL

From the backend project directory:

```bash
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` and creates two databases:

- `tasks` — application development database
- `tasks_test` — integration test database

The databases use the local development credentials defined in `compose.yaml`.

**Note:** PostgreSQL initialisation scripts only run when the database volume is first created. If an existing volume predates the test database configuration, `tasks_test` may need to be created manually.

### 2. Start the application

```bash
./gradlew bootRun
```

The application runs at:

`http://localhost:4000`

Flyway applies database migrations during application startup, and Hibernate validates the resulting schema.

### 3. API documentation

Swagger UI is available at:

`http://localhost:4000/swagger-ui/index.html`

API endpoint documentation will be expanded as the task management functionality is implemented.

## Database

The database schema is managed by versioned Flyway migrations in:

`src/main/resources/db/migration/`

The initial schema includes:

**Caseworkers**

- A unique identifier
- A name

**Tasks**

- A unique identifier
- A required title
- An optional description
- A status (`TODO`, `IN_PROGRESS`, `DONE`)
- A required due date/time
- A reference to the creating caseworker
- Creation and last-updated timestamps

Each task references one caseworker through a foreign key. Deleting a caseworker is restricted while tasks reference them.

The initial migrations also seed three demonstration caseworkers: Danger Mouse, Count Duckula and Super Ted.

Dates and times are represented as UTC instants in Java and stored using PostgreSQL `TIMESTAMPTZ`.

## Testing

Repository integration tests use a separate `tasks_test` database.

Ensure PostgreSQL is running, then execute:

```bash
./gradlew integration --tests "uk.gov.hmcts.reform.dev.repositories.*"
```

The tests verify that seeded caseworkers can be retrieved and that tasks can be persisted and reloaded successfully.

Task persistence tests use transactions that roll back after execution to avoid retaining test data.

## Configuration

The application uses local development database defaults, which can be overridden using environment variables:

- `DB_USER_NAME` — PostgreSQL username
- `DB_PASSWORD` — PostgreSQL password

The integration test configuration is located at:

`src/integrationTest/resources/application-test.yaml`

This directs integration tests to the separate test database.

## Implementation Notes

- Flyway owns database schema creation and migration.
- Hibernate is configured with `ddl-auto: validate` rather than automatically creating or modifying tables.
- Task status is represented by a Java enum and constrained at database level.
- Task creation and update timestamps are managed using JPA lifecycle callbacks.
- Database-generated identity values are used for primary keys.

## Further Development

The following sections will be completed as implementation progresses:

- REST API endpoints and example requests
- Request validation and error responses
- Task management business rules
- API and service-layer testing
- Frontend setup and integration
- Assumptions, limitations and potential improvements
