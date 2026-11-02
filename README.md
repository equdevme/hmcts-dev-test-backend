
# Task Management API

Backend application for the HMCTS developer coding challenge, built with Java, Spring Boot, PostgreSQL and Gradle.

For the cross-application architecture, assumptions and trade-offs, see the [design document](docs/design.md).

**Companion frontend:** [HMCTS Dev Test Frontend](https://github.com/equdevme/hmcts-dev-test-frontend)

The frontend and backend are intended to run together as a single task-management application.

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

The schema includes four related entities:

**Caseworkers**

- A unique identifier and name.
- Caseworkers can own cases and create tasks.

**Cases**

- A unique identifier, required title and optional description.
- A required owner (`owner_id`) referencing a caseworker.
- One caseworker can own multiple cases.

**Tasks**

- A unique identifier, required title and optional description.
- A required status referencing the `task_statuses` table.
- A required due date/time.
- A required case reference (`case_id`) and a separate reference to the creating caseworker (`created_by`).
- Creation and last-updated timestamps.

**Task statuses**

- A unique status code (`code`) used as a stable identifier.
- A human-readable display label (`label`).
- A display order (`display_order`) determining the order in which statuses are presented.
- A default flag (`is_default`) identifying the status assigned to newly created tasks.

The initial task statuses are:

| Code | Label | Display order | Default |
|---|---|---|---|
| `TODO` | To do | 1 | Yes |
| `IN_PROGRESS` | In progress | 2 | No |
| `DONE` | Done | 3 | No |

Task records reference status codes through a foreign key to `task_statuses`. This ensures that tasks can only reference statuses defined in the database.

Status codes are stable identifiers used by the API, while labels, ordering and the default status are database-configured. This allows the available statuses and their presentation to be updated through database migrations without changing the Java status model.

A PostgreSQL partial unique index ensures that no more than one status can be marked as the default. Integration tests additionally verify that exactly one default status is configured.

Each task belongs to one case. A case's owner need not be the creator of its tasks: all caseworkers can view and work with tasks across cases. Foreign keys restrict deletion of referenced cases and caseworkers.

Flyway seeds three demonstration caseworkers (Danger Mouse, Count Duckula and Super Ted) and nine demonstration cases, three owned by each caseworker.

Dates and times are represented as UTC instants in Java and stored using PostgreSQL `TIMESTAMPTZ`.

## Case API

Cases are read-only through the current API. Case creation, editing and deletion are outside the scope of this implementation.

### Retrieve cases

**`GET /cases`**

Returns all cases, ordered by ID ascending. Optionally filter by owning caseworker using `ownerId`:

`GET /cases?ownerId=1`

**Successful response — `200 OK`**

```json
[
  {
    "id": 1,
    "ownerId": 1,
    "title": "Example case",
    "description": "Example case description"
  }
]
```

The response is an array, including an empty array (`[]`) when no cases match. IDs and example content are illustrative.

### Retrieve a case by ID

**`GET /cases/{id}`**

Returns the specified case using the same object structure as the case list.

- `200 OK` — Case found.
- `404 Not Found` — Case does not exist.

### Retrieve tasks belonging to a case

**`GET /cases/{id}/tasks`**

Returns tasks associated with the specified case, ordered by due date ascending and then task ID ascending. Each task uses the same response structure as `GET /tasks`.

- `200 OK` — JSON array of tasks, or `[]` if the case has no tasks.
- `404 Not Found` — Case does not exist.

## Task Status API

### Retrieve available task statuses

**`GET /task-statuses`**

Returns the task statuses configured in the database, ordered by `displayOrder` ascending.

**Successful response — `200 OK`**

```json
[
  {
    "code": "TODO",
    "label": "To do",
    "displayOrder": 1,
    "isDefault": true
  },
  {
    "code": "IN_PROGRESS",
    "label": "In progress",
    "displayOrder": 2,
    "isDefault": false
  },
  {
    "code": "DONE",
    "label": "Done",
    "displayOrder": 3,
    "isDefault": false
  }
]
```

These values reflect the initial database configuration.

The endpoint allows API clients to retrieve available status codes, display labels, ordering and default-status information without maintaining a separate hardcoded list.

Status configuration is managed through database migrations; the API does not currently provide endpoints for creating, editing or deleting status definitions.

## Task API

### Create a task

**`POST /tasks`**

Creates a new task associated with an existing case and creating caseworker.

Example request:

```json
{
  "caseId": 1,
  "createdBy": 1,
  "title": "Review case documents",
  "description": "Check submitted evidence",
  "dueAt": "2026-11-05T12:00:00Z"
}
```

**Request fields**

| Field | Required | Description |
|---|---|---|
| `caseId` | Yes | ID of an existing case |
| `createdBy` | Yes | ID of an existing caseworker |
| `title` | Yes | Non-blank task title, maximum 255 characters |
| `description` | No | Optional task description |
| `dueAt` | Yes | ISO 8601 date/time representing an absolute instant |

The API accepts valid past or future due dates, allowing retrospective task recording. The companion frontend applies a stricter rule requiring future due dates for newly created tasks.
New tasks are automatically assigned the status configured as the default in the `task_statuses` table (initially `TODO`).

**Successful response — `201 Created`**

The response contains the created task, including its generated ID, case reference, initial status, creating caseworker reference and timestamps.

Example response:

```json
{
  "id": 4,
  "caseId": 1,
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
| `404 Not Found` | The specified case or caseworker does not exist |

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

A request referencing a nonexistent case or caseworker returns `404 Not Found` with a message identifying the missing resource and ID. A missing `caseId` returns `400 Bad Request`.

### Retrieve tasks

**`GET /tasks`**

Retrieves a list of tasks, optionally filtered by status, creating caseworker and due date/time.

All tasks are visible to all caseworkers. The `createdBy` parameter can be used to retrieve tasks created by a particular caseworker.

**Query parameters**

| Parameter | Required | Description |
|---|---|---|
| `status` | No | Filter by a configured task status code. Available codes can be retrieved using `GET /task-statuses` |
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

Unknown task status codes and incompatible date/time values are also rejected with `400 Bad Request`.

### Retrieve a task by ID

**`GET /tasks/{id}`**

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
| `status` | Yes | New task status code. Must reference an existing status in `task_statuses` |

Available status codes can be retrieved using `GET /task-statuses`.

**Successful response — `200 OK`**

Returns the updated task using the same response structure as `POST /tasks`, including the new status and updated timestamp.

**Error responses**

| Status | Description |
|---|---|
| `400 Bad Request` | Missing, null or unknown status, malformed JSON or unsupported request fields |
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

Integration tests use the `test` Spring profile and the separate `tasks_test` database. Start PostgreSQL before running them.

Run the standard unit-test suite:

```bash
./gradlew test
```

Run the integration-test suite:

```bash
./gradlew integration
```

Run integration tests for a particular class:

```bash
./gradlew integration --tests '*TaskStatusControllerTest'
```

The tests cover the task and case APIs, validation and error responses, filtering and sorting, status reference data, and persistence. The task status tests verify the configured default and display order.

Integration test reports are written to `build/reports/tests/integration/`. The integration test database configuration is in `src/integrationTest/resources/application-test.yaml`.

## Configuration

The application uses local development database defaults, which can be overridden using environment variables:

- `DB_USER_NAME` — PostgreSQL username
- `DB_PASSWORD` — PostgreSQL password

## Further Development

Potential extensions include task assignment, status-transition rules, general task editing and pagination. The design document describes these and other cross-application considerations.
