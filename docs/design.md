# HMCTS Developer Challenge — Design Document

> This is the cross-application design record for the frontend and backend repositories. It describes the current implementation and the reasoning behind key decisions; endpoint details and setup instructions live in the respective READMEs.

## 1. Requirements and scope

### 1.1 Challenge overview

Develop a task management application enabling HMCTS caseworkers to create, view, update and delete tasks, using the supplied backend and frontend starter projects. The implementation aims to demonstrate sound data modelling, separation of concerns, validation, automated testing and maintainability.

### 1.2 Core functional requirements

**Backend:** Create tasks with a title, optional description, status and due date/time; retrieve individual and multiple tasks; update task status; delete tasks.

**Frontend:** Create tasks; display tasks and their details; update status; delete tasks.

### 1.3 Non-functional requirements

- **Persistence:** Retain data between application restarts.
- **Validation:** Reject invalid input and provide meaningful feedback.
- **Error handling:** Handle expected failures consistently.
- **Testing:** Cover core operations and representative failure scenarios.
- **Documentation:** Explain setup, API behaviour, assumptions and design decisions.
- **Maintainability:** Keep responsibilities separated and changes straightforward.
- **Accessibility:** Use semantic, accessible forms and controls; do not make JavaScript-only interactions the sole route to completing a task.

### 1.4 Scope

**Implemented:** Core task creation, retrieval, status update and deletion; PostgreSQL persistence; caseworkers and cases; case-based navigation; simulated caseworker selection; validation and error handling; automated backend and frontend tests; API and setup documentation; frontend case-level task status filtering and due-date sorting using statuses from the backend API.

**Out of scope:** Authentication, production authorisation, case CRUD, task assignment, collaborative editing, general task editing, pagination and approval workflows.

## 2. Questions, assumptions and limitations

### 2.1 Ambiguities in the challenge

The challenge does not fully define whether tasks belong to individual caseworkers, whether they can be shared or assigned, whether creators are also owners, or whether caseworkers can access one another's work. These distinctions affect data modelling and authorisation. Rather than inventing a full permissions model, this implementation adopts a small, explicit set of assumptions.

### 2.2 Working assumptions

**Case ownership and task creation**

- Every task belongs to exactly one case (`tasks.case_id`).
- Every case has one owning caseworker (`cases.owner_id`); one caseworker may own many cases.
- Every task records its original creator (`tasks.created_by`). The creator may differ from the case owner.
- All demonstration caseworkers may view cases and their tasks and perform task operations. Case ownership supports organisation and filtering, not access control.
- Task ownership and assignment are not separately modelled.

**Caseworker identity**

Three demonstration caseworkers are seeded. The frontend lets a user select a caseworker and carries the selected `caseworkerId` in navigation query parameters. This supports a demonstration of case filtering and recording task creation; it is **not authentication** and the backend does not trust it as an authorisation mechanism.

**Task lifecycle**

Deleting a task permanently removes its record. Soft deletion, restoration and archiving are outside scope.

### 2.3 Known limitations

The application has no authentication or production-grade authorisation. Any user able to reach the API could request or modify task data; selecting a demonstration caseworker does not establish identity. A task's creator does not necessarily indicate responsibility for completing it. Each case has only one owner, and there is no separate task assignee. These are deliberate simplifications for the challenge, not production recommendations.

## 3. Architectural decision record

The following decisions retain the rationale and alternatives considered during development. Where the implementation changed from the initial plan, that evolution is noted explicitly.

### Decision 001 — Caseworker relationships and task creation

**Context:** The challenge refers to caseworkers managing tasks without specifying ownership, assignment or collaboration.

**Decision:** Store the original task creator in `tasks.created_by`, a mandatory foreign key to `caseworkers.id`. Case ownership is separate (`cases.owner_id`); a caseworker can create tasks for another caseworker's case. Task creator is not editable through the current API.

**Alternatives considered:** A task `owner_id`; separate creator and owner; a task-to-caseworker mapping table with multiple roles.

**Rationale:** Creation is a well-defined relationship. Assignment and ownership would require business rules not provided by the challenge. Keeping them distinct avoids treating the creator as an implicit assignee.

**Trade-offs and extension:** Filtering by creator does not identify all work assigned to someone. A future relationship table could represent owner, assignee and collaborator roles once requirements are clear. Referenced caseworkers cannot be deleted while tasks retain their creator relationship; a production system might deactivate departed caseworkers rather than delete them.

### Decision 002 — Simulated caseworker identity

**Context:** The frontend needs to associate task creation and case filtering with a caseworker, but authentication is out of scope.

**Original proposal:** Store a selected caseworker ID in an Express session.

**Implemented decision:** Seed demonstration caseworkers and select one through the frontend. Preserve the selection using a `caseworkerId` query parameter across relevant navigation and form actions rather than implementing session persistence.

**Rationale:** Query-parameter context is simple and visible, avoids session configuration for a demonstration-only identity and keeps the focus on task functionality. It also makes the selected context easy to inspect while testing navigation.

**Limitations:** Query parameters are user-controlled and must never be treated as proof of identity. The backend does not enforce caseworker-specific permissions.

**Future extension:** Replace this mechanism with authenticated identity and server-side authorisation checks. A real session could then hold authenticated state rather than a freely selected demonstration identity.

### Decision 003 — Shared task visibility and creator-based filtering

**Context:** The challenge does not establish whether tasks are private to their creator.

**Decision:** Treat tasks as shared across the demonstration caseworkers. `GET /tasks` returns all tasks unless filtered by optional `createdBy`. The frontend primarily organises work by case, and the case list can be filtered by case owner.

**Rationale:** Shared visibility supports the case-oriented workflow without introducing speculative permissions. Creator-based filtering remains useful without conflating creator and assignee.

**Limitations and extension:** This is not an access-control policy suitable for production. Future authenticated roles and assignment rules could restrict visibility and operations.

### Decision 004 — PostgreSQL schema management with Flyway

**Context:** The application requires persistent relational data and repeatable schema evolution.

**Decision:** Use PostgreSQL and versioned Flyway SQL migrations. Configure Hibernate to validate entity mappings rather than generate or update the schema.

**Alternatives considered:** Manual SQL scripts; Hibernate schema generation; Liquibase.

**Rationale:** Explicit SQL migrations make constraints, foreign keys and changes reviewable and reproducible across development and test environments.

**Trade-offs:** Migrations must be maintained and tested, especially when changing existing data. Previously applied migrations should not be rewritten; subsequent changes belong in new migrations.

### Decision 005 — Case-based task organisation and ownership

**Context:** The supplied backend included an example case concept, and a flat task list lacked the domain context in which caseworkers perform work.

**Decision:** Introduce a minimal case model. Each case has a title, optional description and one owner. Each task belongs to one case. Cases and their tasks are visible to all demonstration caseworkers. The frontend offers **All cases** and **My cases** views, case details and case-scoped task creation.

**Alternatives considered:** A flat task list; optional case associations; cases with only a creator; multiple case owners; comprehensive case management.

**Rationale:** Cases provide a useful domain boundary and navigation structure while preserving a manageable implementation scope. Case ownership is deliberately distinct from task creation.

**Referential integrity:** `tasks.case_id` prevents deletion of referenced cases; `cases.owner_id` prevents deletion of referenced caseworkers. Case creation, editing, deletion and reassignment are not exposed by the current API.

**Future extension:** A caseworker-to-case relationship table could support multiple assignments or collaboration roles when needed.

### Decision 006 — Database-managed task statuses

**Context:** An initial Java enum and database `CHECK` constraint represented `TODO`, `IN_PROGRESS` and `DONE`. Maintaining these values and display labels separately in Java, SQL and frontend templates would duplicate configuration and make changes more cumbersome.

**Decision:** Replace the enum with a JPA `TaskStatus` entity backed by a `task_statuses` reference table. Each status has a stable `code`, human-readable `label`, `display_order` and `is_default`. `tasks.status` references `task_statuses.code` through a foreign key. Expose `GET /task-statuses` so clients can obtain configured statuses and labels.

**Alternatives considered:** Retain the enum and `CHECK` constraint; maintain a hardcoded frontend status list; use an unrestricted string status column.

**Rationale:** A reference table centralises valid codes and presentation metadata, enforces referential integrity and allows labels, ordering, default selection and additional statuses to be managed through migrations. The API still exposes stable string codes rather than serialising persistence entities.

**Default status:** Task creation looks up the status marked as default instead of assigning `TODO` in Java. A PostgreSQL partial unique index guarantees **at most one** default; integration tests verify **exactly one** is configured. The database index does not itself guarantee that a default exists, so migrations must maintain that invariant.

**Validation:** Status filters and updates resolve codes through the repository. Unknown codes receive field-specific `400 Bad Request` responses. Task responses continue to return the status code as a string.

**Trade-offs:** The additional table and relationship introduce a database lookup and more configuration to maintain. Changing a code already referenced by tasks requires a deliberate data migration; labels can change without changing those references. The frontend uses the endpoint to display and update statuses as well as populate case-level filters.

## 4. Data model

### 4.1 Relationships

```text
caseworkers (1) ────────< cases (many)
      │                     owner_id
      │
      └───────────────────< tasks (many)
                            created_by

cases (1) ───────────────< tasks (many)
                            case_id

task_statuses (1) ──────< tasks (many)
                            status → code
```

The relationships represent case ownership, original task creation, case membership and task progress as distinct concepts.

### 4.2 `caseworkers`

| Column | Type | Constraints / purpose |
|---|---|---|
| `id` | `BIGINT` | Generated primary key |
| `name` | `VARCHAR(100)` | Required display name |

### 4.3 `cases`

| Column | Type | Constraints / purpose |
|---|---|---|
| `id` | `BIGINT` | Generated primary key |
| `title` | `VARCHAR(255)` | Required case title |
| `description` | `TEXT` | Optional description |
| `owner_id` | `BIGINT` | Required foreign key to `caseworkers.id`; deletion restricted |

### 4.4 `tasks`

| Column | Type | Constraints / purpose |
|---|---|---|
| `id` | `BIGINT` | Generated primary key |
| `case_id` | `BIGINT` | Required foreign key to `cases.id`; deletion restricted |
| `created_by` | `BIGINT` | Required foreign key to `caseworkers.id`; deletion restricted |
| `title` | `VARCHAR(255)` | Required title |
| `description` | `TEXT` | Optional description |
| `status` | `VARCHAR(20)` | Required foreign key to `task_statuses.code` (replaces the original `CHECK` constraint) |
| `due_at` | `TIMESTAMPTZ` | Required due date/time |
| `created_at` | `TIMESTAMPTZ` | Required creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | Required last-update timestamp |

The original task status column is `VARCHAR(20)`; V5 replaces its check constraint with a foreign key but does not widen the column. Although `task_statuses.code` allows up to 50 characters, status codes used by tasks must fit within the 20-character task column.

### 4.5 `task_statuses`

| Column | Type | Constraints / purpose |
|---|---|---|
| `code` | `VARCHAR(50)` | Primary key; stable API identifier |
| `label` | `VARCHAR(100)` | Required human-readable label |
| `display_order` | `INTEGER` | Required display ordering |
| `is_default` | `BOOLEAN` | Required; defaults to false; partial unique index prevents multiple true values |

Initial data:

| Code | Label | Order | Default |
|---|---|---:|---|
| `TODO` | To do | 1 | Yes |
| `IN_PROGRESS` | In progress | 2 | No |
| `DONE` | Done | 3 | No |

### 4.6 Flyway migrations

| Version | Purpose |
|---|---|
| V1 | Create the original caseworker and task schema |
| V2 | Seed the demonstration caseworkers |
| V3 | Introduce cases and case-task associations |
| V4 | Add case ownership and seed additional demonstration cases |
| V5 | Create and seed `task_statuses`; replace the task status `CHECK` constraint with a foreign key |
| V6 | Add `is_default`, configure the initial default and enforce at most one default through a partial unique index |

Migration files in `src/main/resources/db/migration/` are the authoritative record of exact SQL and constraints. Later migrations evolve the schema without editing migrations already applied.

### 4.7 Referential integrity and lifecycle

Database foreign keys protect the relationships between tasks, cases, caseworkers and configured statuses. Tasks are physically deleted when requested; case and caseworker deletion is restricted where referenced. Status codes in use cannot simply be removed without first addressing dependent tasks. Timestamps are represented as Java `Instant` values and stored in PostgreSQL `TIMESTAMPTZ` columns.

## 5. API design

### 5.1 Endpoint overview

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/caseworkers` | Retrieve demonstration caseworkers |
| `GET` | `/cases` | List cases; optional `ownerId` filter |
| `GET` | `/cases/{id}` | Retrieve case details |
| `GET` | `/cases/{id}/tasks` | Retrieve tasks for one case |
| `GET` | `/task-statuses` | Retrieve configured statuses in display order |
| `POST` | `/tasks` | Create a task |
| `GET` | `/tasks` | List, filter and sort tasks |
| `GET` | `/tasks/{id}` | Retrieve one task |
| `PATCH` | `/tasks/{id}` | Update task status |
| `DELETE` | `/tasks/{id}` | Delete a task |

The README provides complete request and response examples. Swagger UI exposes the implemented API interactively.

### 5.2 Request and response boundaries

Controllers accept request DTOs and map domain entities to response DTOs. Java records provide concise API contracts while keeping JPA entities separate. Task responses include the status **code**, not the `TaskStatus` entity. `POST /tasks` requires a case, creator, title and due date; the server assigns the configured default status. `PATCH /tasks/{id}` accepts only a status change.

### 5.3 Validation and error handling

Jakarta Bean Validation handles required fields and other input constraints. Services verify referenced caseworkers, cases and statuses. Query parameters are validated, including supported sort fields and directions. Unknown statuses are rejected rather than silently accepted.

Centralised exception handling uses Spring `ProblemDetail` responses, with field-specific `errors` entries where appropriate. Invalid requests return `400`; missing referenced resources return `404`; successful creation returns `201`; successful deletion returns `204` without a body.

### 5.4 Filtering and ordering

`GET /tasks` supports optional `status`, `createdBy` and `dueBefore` filters and a restricted set of sort fields and directions. Spring Data JPA Specifications compose optional predicates. Supported sort fields are explicitly constrained rather than accepting arbitrary property names; a secondary task ID sort provides deterministic ordering. Case-scoped task retrieval orders by due date and ID.

## 6. Implementation approach

### 6.1 Backend architecture

The backend uses Java 21, Spring Boot, Spring Data JPA, PostgreSQL and Gradle. Its principal layers are:

```text
HTTP request
    ↓
Controller — request handling, validation boundary, response mapping
    ↓
Service — business operations and transactions
    ↓
Repository / Specification — persistence and querying
    ↓
JPA entities / PostgreSQL — relational state
```

DTOs keep API contracts distinct from persistence mappings. A central exception handler produces consistent errors. Flyway controls schema changes and Hibernate validates the schema at startup.

### 6.2 Persistence and transactions

Spring Data repositories provide entity access and derived queries; Specifications compose task-list filters. Service methods perform task creation, status changes and deletion. Status updates use JPA-managed entities and dirty checking; lifecycle callbacks maintain timestamps. Referential integrity is also enforced in PostgreSQL rather than relying solely on application validation.

### 6.3 Frontend architecture

The frontend uses Express, TypeScript and Nunjucks with GOV.UK Design System patterns. Server-rendered pages cover caseworker selection, case lists and details, task creation and details, status changes and deletion confirmation.

Navigation preserves the selected demonstration caseworker through `caseworkerId`. Task creation includes validation feedback and converts entered local date/time into an unambiguous instant, accounting for UK time-zone transitions. Task status changes support a progressively enhanced interaction: the essential operation remains available without relying exclusively on browser-side JavaScript. Task deletion uses an explicit confirmation step with contextual navigation.

Case-level task status filtering uses the configured statuses returned by `GET /task-statuses`, and tasks can be sorted by due date in either direction. The task details page also uses the configured status codes and labels for its status display and update dropdown. The Express route validates submitted updates against the backend's configured status codes.

### 6.4 Accessibility considerations

The interface follows GOV.UK form and content patterns, with labelled controls, validation feedback and confirmation steps. Progressive enhancement is preferred so that JavaScript augments rather than replaces core actions. A proposed drag-and-drop board would need a keyboard- and assistive-technology-accessible alternative for changing status.

### 6.5 Local environment and documentation

Docker Compose runs PostgreSQL locally, including a separate integration-test database. The backend runs on port 4000 and the frontend is served locally over HTTPS. Setup instructions, endpoint examples and test commands are maintained in the respective READMEs; Swagger UI supports API exploration.

## 7. Testing

### 7.1 Strategy

Testing combines backend repository and controller integration tests with frontend route tests. Tests focus on successful operations, persistence, validation, filtering, sorting and error responses. The backend integration suite runs against the separate `tasks_test` PostgreSQL database.

### 7.2 Backend tests

Repository integration tests cover persistence and relationships for caseworkers, cases and tasks. Task-status repository tests verify retrieval in display order, default lookup and the existence of exactly one configured default.

MockMvc controller integration tests exercise task creation, retrieval, status updates and deletion; case and caseworker endpoints; filtering and sorting; and representative invalid requests. The status controller test checks that `GET /task-statuses` returns configured codes, labels, order and default flags.

**Verification:** The full backend integration suite was recorded as passing on 29 October 2026 using:

```bash
./gradlew integration
```

The earlier run reported 50 tests with one outdated assertion; after correcting the assertion to compare status codes, Gradle reported `BUILD SUCCESSFUL`. A targeted run of the task and case controller integration tests also passed on 2 November 2026. Rerun the full suite before submission.

### 7.3 Frontend tests

The frontend uses Jest and Supertest for route-level behaviour. Tests cover navigation, task operations, creation validation (including the 255-character title limit), status filtering and sorting, status update validation, and deletion. The route suite passed on 2 November 2026. Run the frontend build, linting, formatting and test checks again before submission.

## 8. Future considerations

### 8.1 Authentication and authorisation

Replace the demonstration selector with verified identity. Define access rules for viewing cases, creating tasks and modifying or deleting work. Enforce these rules on the backend rather than relying on frontend filtering.

### 8.2 Task proposal and approval

Allow a caseworker to propose a task for a colleague's case, with the case owner able to approve or reject it. Model approval independently from progress status so that `TODO`, `IN_PROGRESS` and `DONE` retain their existing meaning.

### 8.3 Kanban-style task board

Introduce status columns generated from `GET /task-statuses`. Dragging a task between columns could call the existing `PATCH /tasks/{id}` endpoint. Provide equivalent keyboard and accessible form controls; drag-and-drop must not be the only way to update a task.

### 8.4 Other enhancements

- Task assignment and collaboration roles.
- Explicit allowed status transitions, if business rules require them.
- Editing task fields beyond status.
- Pagination for larger datasets.
- Backend due-date business rules, if required.
- Auditing, soft deletion or restoration if the operational context calls for them.

### 8.5 Technical limitations

This is a challenge implementation rather than a production case management system. Its simplified identity model, shared visibility, single case owner and limited task lifecycle are intentional scope decisions. The database-managed status model also requires care when changing codes already referenced by tasks; migrations must preserve referential integrity and a valid default.

## Appendix — Reference material

- [Backend README](../README.md) — local setup, API reference and backend test commands.
- [Frontend README](https://github.com/equdevme/hmcts-dev-test-frontend/blob/master/README.md) — frontend setup and checks.
- Flyway migrations: `src/main/resources/db/migration/`.
- Original HMCTS challenge instructions and starter repositories (refer to the supplied challenge materials).
