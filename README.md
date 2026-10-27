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

The implemented API endpoints can be explored and tested interactively through Swagger UI.

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

### Retrieve tasks

**`GET /tasks`**

Retrieves a list of tasks, optionally filtered by status, creating caseworker and due date/time.

All tasks are visible to all caseworkers. The `createdBy` parameter can be used to retrieve tasks created by a particular caseworker.

**Query parameters**

| Parameter | Required | Description |
|---|---|---|
| `status` | No | Filter by task status: `TODO`, `IN_PROGRESS` or `DONE` |
| `createdBy` | No | Filter by the ID of the creating caseworker |
| `dueBefore` | No | Return tasks due strictly before the specified ISO 8601 instant |
| `sortBy` | No | Sort by `dueAt`, `createdAt`, `updatedAt`, `title` or `status`. Defaults to `dueAt` |
| `sortDirection` | No | `asc` or `desc` (case-insensitive). Defaults to `asc` |

Filters can be combined. Tasks are sorted by the requested field and direction, with task ID ascending as a secondary sort to ensure deterministic ordering.

**Example requests**

Retrieve all tasks using the default due-date ordering:

`GET /tasks`

Retrieve tasks created by caseworker 1:

`GET /tasks?createdBy=1`

Retrieve incomplete tasks due before a particular date, ordered by due date:

`GET /tasks?status=TODO&dueBefore=2026-11-05T12:00:00Z&sortBy=dueAt&sortDirection=asc`

Retrieve tasks sorted alphabetically by title:

`GET /tasks?sortBy=title&sortDirection=asc`

**Successful response — `200 OK`**

Returns a JSON array of task objects, using the same response fields as `POST /tasks`.

When no tasks match the supplied filters, the response is an empty array (`[]`) with status `200 OK`.

**Error responses**

| Status | Description |
|---|---|
| `400 Bad Request` | An invalid filter value, incompatible parameter type, unsupported sort field or invalid sort direction |

Invalid query parameters return a `ProblemDetail` response containing field-specific errors.

For example, `GET /tasks?sortDirection=sideways` returns:

```json
{
  "title": "Invalid query parameter",
  "status": 400,
  "detail": "Invalid query parameter: sortDirection",
  "errors": {
    "sortDirection": "Must be 'asc' or 'desc'"
  }
}
```

Invalid enum values and incompatible date/time values are also rejected with `400 Bad Request`.

### Retrieve a task by ID

`GET /tasks/{id}`

Retrieves a single task by its ID.

**Example:**

```http
GET /tasks/1
```

**Responses:**
- `200 OK` — Returns the task using the same response structure as task creation.
- `404 Not Found` — The task does not exist, returned as a `ProblemDetail` response.
- `400 Bad Request` — The task ID is not a valid number.

### Update a task's status

**`PATCH /tasks/{id}`**

Updates the status of an existing task. Only the `status` field can currently be updated; other task properties are not editable through this endpoint.

**Example request:**

```json
{
  "status": "IN_PROGRESS"
}
```

**Request fields**

| Field | Required | Description |
|---|---|---|
| `status` | Yes | New task status: `TODO`, `IN_PROGRESS` or `DONE` |

**Successful response — `200 OK`**

Returns the updated task using the same response structure as `POST /tasks`, including the new status and updated timestamp.

**Error responses**

| Status | Description |
|---|---|
| `400 Bad Request` | Missing, null or invalid status, malformed JSON or unsupported request fields |
| `404 Not Found` | The specified task does not exist |

Errors use the existing `ProblemDetail` response format.

### Delete a task

**`DELETE /tasks/{id}`**

Permanently deletes an existing task.

**Successful response — `204 No Content`**

The task is deleted and the response contains no body.

**Error responses**

| Status | Description |
|---|---|
| `400 Bad Request` | The task ID is not a valid number |
| `404 Not Found` | The specified task does not exist |

Deleting a task that does not exist returns `404 Not Found`. Repeated deletion requests remain idempotent in their effect on the database.

## Caseworker API

### Retrieve caseworkers

**`GET /caseworkers`**

Returns all caseworkers from the database, including their IDs and names.

The application seeds three demonstration caseworkers: Danger Mouse, Count Duckula and Super Ted. These are used to simulate the currently selected caseworker in the frontend without implementing authentication.

**Successful response — `200 OK`**

```json
[
  { "id": 1, "name": "Danger Mouse" },
  { "id": 2, "name": "Count Duckula" },
  { "id": 3, "name": "Super Ted" }
]
```

The IDs are illustrative and are generated by the database.

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

To run only the caseworker controller integration tests:

```bash
./gradlew integration --tests "uk.gov.hmcts.reform.dev.controllers.CaseworkerControllerTest"
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

The task retrieval tests cover:

- Retrieving tasks with their creating caseworker IDs and default due-date ordering.
- Filtering tasks by creating caseworker, status and due date/time.
- Combining multiple filters.
- Sorting tasks by due date in ascending and descending order.
- Sorting tasks alphabetically by title.
- Accepting case-insensitive sort directions.
- Rejecting invalid filter and sorting parameters with `400 Bad Request`.
- Returning an empty array when no tasks match the supplied filters.

The individual task retrieval tests cover:

- Retrieving an existing task by ID.
- Returning `404 Not Found` when the task does not exist.

The task status update tests cover:

- Successfully updating a task's status and persisting the change.
- Updating the task's `updatedAt` timestamp.
- Rejecting missing, null or invalid status values with `400 Bad Request`.
- Rejecting unsupported request fields with `400 Bad Request`.
- Returning `404 Not Found` when updating a nonexistent task.

The task deletion tests cover:

- Successfully deleting a task and returning `204 No Content` with an empty response body.
- Verifying that the deleted task no longer exists in the database.
- Returning `404 Not Found` when deleting a nonexistent task.

The caseworker retrieval test verifies that `GET /caseworkers` returns the three seeded demonstration caseworkers.

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
- Controllers handle HTTP request parameters and map domain entities to API response DTOs; services implement task operations without depending on request or response DTOs.
- Task creation resolves the creating caseworker and persists the task through Spring Data JPA repositories.
- Task retrieval uses Spring Data JPA Specifications to compose optional filters without requiring separate repository methods for each combination.
- Sorting is restricted to an explicit enum of supported task fields, with a secondary ID sort for deterministic results.
- Exception handling is centralised using `@RestControllerAdvice` and Spring's `ProblemDetail` responses, including field-specific validation and query-parameter errors.
- Task status updates are restricted to the `status` field through `PATCH /tasks/{id}`. Broader task editing is outside the current scope.
- Status updates use JPA dirty checking within a transaction, with the `@PreUpdate` lifecycle callback maintaining the last-updated timestamp.
- Task deletion permanently removes the record and returns `404 Not Found` when the specified task does not exist.
- Jackson is configured to reject unknown JSON properties, preventing unsupported request fields from being silently ignored.

## Further Development

The following functionality and improvements are outside the current backend implementation scope or remain to be completed:

- Frontend application for creating, viewing, updating task statuses and deleting tasks.
- Additional task management business rules, such as status transition restrictions.
- General-purpose task editing beyond status updates.
- Pagination for task listing if required for larger datasets.
- Further documentation of assumptions, limitations and potential improvements.
