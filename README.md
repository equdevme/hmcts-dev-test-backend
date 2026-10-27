# Task Management API

Backend application for the HMCTS developer coding challenge, built with Java, Spring Boot, PostgreSQL and Gradle.

## Technology Stack

- Java 21
- Spring Boot
- Gradle
- PostgreSQL 17
- Spring Data JPA / Hibernate
- Flyway for database migrations
- Jakarta Bean Validation for request validation
- JUnit, AssertJ and Spring MockMvc for testing
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

The implemented API endpoints can be explored and tested interactively through Swagger UI. Documentation will be expanded as additional task management functionality is implemented.

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

## Task API

### Create a task

**`POST /tasks`**

Creates a new task associated with an existing caseworker.

Example request:

```json
{
  "createdBy": 1,
  "title": "Review case documents",
  "description": "Check submitted evidence",
  "dueAt": "2026-11-05T12:00:00Z"
}
```

**Request fields**

| Field | Required | Description |
|---|---|---|
| `createdBy` | Yes | ID of an existing caseworker |
| `title` | Yes | Non-blank task title, maximum 255 characters |
| `description` | No | Optional task description |
| `dueAt` | Yes | ISO 8601 date/time representing an absolute instant |

New tasks are assigned the `TODO` status automatically.

**Successful response — `201 Created`**

The response contains the created task, including its generated ID, initial status, caseworker reference and timestamps.

Example response:

```json
{
  "id": 4,
  "createdBy": 1,
  "title": "Review case documents",
  "description": "Check submitted evidence",
  "status": "TODO",
  "dueAt": "2026-11-05T12:00:00Z",
  "createdAt": "2026-10-27T10:00:00Z",
  "updatedAt": "2026-10-27T10:00:00Z"
}
```

The ID and timestamps above are illustrative; actual values are generated when the task is persisted.

**Error responses**

| Status | Description |
|---|---|
| `400 Bad Request` | Invalid JSON, incompatible field values or request validation failure |
| `404 Not Found` | The specified caseworker does not exist |

Errors are returned using Spring's `ProblemDetail` response format.

Validation failures include field-specific error messages. For example, a blank title returns a `400` response containing:

```json
{
  "title": "Validation failed",
  "status": 400,
  "detail": "Request validation failed",
  "errors": {
    "title": "must not be blank"
  }
}
```

JSON deserialization failures also identify the offending field where available.

For example, supplying `"abcd"` as `createdBy` returns a `400` response with an error associated with `createdBy`.

A request referencing a nonexistent caseworker returns `404 Not Found` with a message identifying the missing caseworker ID.

## Testing

Integration tests use a separate `tasks_test` database.

Ensure PostgreSQL is running, then execute all integration tests:

```bash
./gradlew integration
```

To run only the repository integration tests:

```bash
./gradlew integration --tests "uk.gov.hmcts.reform.dev.repositories.*"
```

To run only the task controller integration tests:

```bash
./gradlew integration --tests "uk.gov.hmcts.reform.dev.controllers.TaskControllerTest"
```

### Repository integration tests

These verify that:

- Seeded caseworkers can be retrieved.
- Tasks can be persisted and reloaded successfully.
- Persisted task fields, relationships, default status and timestamps are populated as expected.

### Controller integration tests

Controller integration tests use Spring MockMvc to exercise the API request handling, validation, service and persistence layers.

The task creation tests cover:

- Successful task creation returning `201 Created`.
- Rejection of a blank title with `400 Bad Request`.
- Rejection of a missing due date with `400 Bad Request`.
- Rejection of an incompatible caseworker ID value with `400 Bad Request`.
- Rejection of a nonexistent caseworker with `404 Not Found`.

Controller tests create their own caseworker test data rather than relying on hardcoded database IDs.

Tests use transactions that roll back after execution to avoid retaining test data.

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
- API request and response representations use Java records, keeping the persistence entities separate from the API contract.
- Request validation uses Jakarta Bean Validation annotations.
- The controller delegates task creation to a service, which resolves the caseworker and persists the task through Spring Data JPA repositories.
- Exception handling is centralised using `@RestControllerAdvice` and Spring's `ProblemDetail` responses.

## Further Development

The following functionality and documentation will be completed as implementation progresses:

- Remaining task retrieval, update and deletion endpoints
- Additional task management business rules
- Integration tests for the remaining API endpoints
- Frontend setup and integration
- Assumptions, limitations and potential improvements
