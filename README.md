# Notes Vault API

[![CI](https://github.com/theruviparambil/notes-vault/actions/workflows/ci.yml/badge.svg)](https://github.com/theruviparambil/notes-vault/actions/workflows/ci.yml)

A small REST service for creating, reading, and deleting notes. Built for the
Bluestaq backend coding challenge.

## Quick start

Run the full stack with a single command:

```bash
docker compose up --build
```

PostgreSQL and the application start together. The API is available at
http://localhost:8080.

If your machine already has PostgreSQL listening on port 5432, change the
host port in `compose.yaml` (for example, `5499:5432`). The application
container connects to PostgreSQL on the docker network, so only the host
port matters.

### Local development

To run the application on the host while keeping the database in Docker:

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

The Maven wrapper (`mvnw`) is bundled, so only JDK 21 is required.

### Run the tests

```bash
./mvnw verify
```

There are 19 tests across four classes:

- `NoteServiceTest` — service-layer unit tests with Mockito.
- `NoteControllerTest` — HTTP-layer tests using `@WebMvcTest`.
- `NoteRepositoryTest` — repository tests using `@DataJpaTest` against a
  PostgreSQL container provisioned by Testcontainers.
- `NotesApiIntegrationTest` — end-to-end tests using `@SpringBootTest` and
  Testcontainers PostgreSQL.

A working Docker daemon is required for the Testcontainers tests. The same
suite runs in CI on every push.

## API

| Method | Path           | Description                                                       |
|--------|----------------|-------------------------------------------------------------------|
| POST   | `/notes`       | Create a note. Returns 201 on success, 400 on validation failure. |
| GET    | `/notes`       | List all notes, sorted newest first. Returns 200.                 |
| GET    | `/notes/{id}`  | Get a note by id. Returns 200, 400, or 404.                       |
| DELETE | `/notes/{id}`  | Delete a note by id. Returns 204, 400, or 404.                    |

Additional endpoints:

- `/swagger-ui.html` — interactive API documentation
- `/v3/api-docs` — OpenAPI 3 JSON document
- `/actuator/health/{liveness,readiness}` — health probes

Error responses follow [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807) with
the `application/problem+json` content type.

### Examples

Create a note:

```http
POST /notes
Content-Type: application/json

{"content": "Mission brief"}
```

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/notes/019ddc2e-faa7-7c91-be3e-1900a4d6664e
Content-Type: application/json

{
  "id":        "019ddc2e-faa7-7c91-be3e-1900a4d6664e",
  "content":   "Mission brief",
  "createdAt": "2026-04-30T02:19:19.615611Z"
}
```

Validation failure:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type":   "https://api.bluestaq.example/errors/validation-failed",
  "title":  "Validation failed",
  "status": 400,
  "detail": "Validation failed",
  "errors": [{"field": "content", "message": "content must not be blank"}]
}
```

Note not found:

```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "type":   "https://api.bluestaq.example/errors/note-not-found",
  "title":  "Note not found",
  "status": 404,
  "detail": "Note not found: 019ddcc9-947f-7245-b31f-57c03436c364",
  "noteId": "019ddcc9-947f-7245-b31f-57c03436c364"
}
```

## System overview

The service is a single Spring Boot application backed by PostgreSQL. The
request path is:

```
client → NoteController → NoteService → NoteRepository → PostgreSQL
```

The code is organized by feature rather than by layer. All notes-related
classes live in one package; only the controller and the request/response
records are public, so internals stay encapsulated and a future feature
becomes a sibling package.

```
src/main/java/com/bluestaq/notesvault/
├── NotesVaultApplication.java
├── api/error/GlobalExceptionHandler.java
├── config/JpaAuditingConfig.java
├── config/OpenApiConfig.java
└── notes/
    ├── Note.java                  # JPA entity
    ├── NoteRepository.java        # Spring Data JPA repository
    ├── NoteService.java           # business logic, transactions, audit
    ├── NoteController.java        # HTTP layer
    ├── NoteRequest.java           # request DTO (validated)
    ├── NoteResponse.java          # response DTO
    └── NoteNotFoundException.java
```

The schema is owned by Flyway (`src/main/resources/db/migration/V1__init.sql`)
and validated by Hibernate at startup (`spring.jpa.hibernate.ddl-auto=validate`),
so the application refuses to start if the entity mapping and the schema
disagree.

## Tech choices

| Layer        | Choice                                              |
|--------------|-----------------------------------------------------|
| Language     | Java 21 (LTS)                                       |
| Framework    | Spring Boot 3.4.5                                   |
| Build        | Maven 3.9 with bundled wrapper                      |
| Database     | PostgreSQL 16                                       |
| Persistence  | Spring Data JPA / Hibernate                         |
| Migrations   | Flyway                                              |
| ID strategy  | UUIDv7 (via the `uuid-creator` library)             |
| Validation   | Jakarta Bean Validation                             |
| Errors       | RFC 7807 Problem Details                            |
| Logging      | SLF4J with ECS-format structured JSON               |
| Health       | Spring Boot Actuator                                |
| API docs     | springdoc-openapi (OpenAPI 3 + Swagger UI)          |
| Tests        | JUnit 5, AssertJ, Mockito, Testcontainers           |
| Run          | Docker Compose                                      |

## Assumptions, trade-offs, and future improvements

### Assumptions

- Notes are not user-scoped. The API has no concept of ownership; any client
  can list, read, or delete any note.
- Note content is plain text, up to 10,000 characters.
- IDs and `createdAt` are generated server-side. Clients cannot supply or
  modify them.

### Trade-offs

**PostgreSQL instead of in-memory storage.** The brief allowed in-memory, but
using a real database means tests catch dialect-specific behavior (timestamp
precision, constraints, SQL semantics) that an in-memory store would mask.

**UUIDv7 IDs, generated in the application.** The time-ordered prefix keeps
PostgreSQL B-tree indexes packed during inserts. UUIDv4 would fragment the
index; integer IDs would leak the volume and creation order of notes.

**Separate request/response DTOs from the JPA entity.** Prevents clients from
setting fields they shouldn't (`id`, `createdAt`) and decouples the wire
format from the database schema. Adds about thirty lines of mapping code.

**Flyway with `ddl-auto=validate`.** Flyway owns the schema. Hibernate
verifies its mapping matches at application startup. This catches drift
between the entity model and the migration files at boot rather than at
runtime.

**`@Version` on the entity.** Optimistic locking is wired in even though the
API does not currently expose an UPDATE endpoint. Adds one column and
positions the API to handle concurrent writes when one is added.

**Hard delete.** A soft-delete column with filtered queries was not
requested and is real complexity. Deletes are irreversible from the API;
restoration would come from a database backup.

**`DELETE` returns 404 for missing IDs**, not 204. RFC 7231 allows either.
Returning 404 surfaces buggy clients earlier rather than silently accepting
no-op deletes.

**Virtual threads enabled** (`spring.threads.virtual.enabled=true`). For an
I/O-bound CRUD service on Spring Boot 3.2+, virtual threads improve
throughput without code changes.

**No Lombok.** Records cover most of the boilerplate. The remaining JPA
entity uses hand-written getters.

### Future improvements

- **Authentication and authorization.** An OAuth2 resource server in front
  of the controller, an `owner_id` column on the entity, and per-user
  access checks in the service. The audit log is already structured to
  record a principal.
- **Pagination on `GET /notes`.** Cursor-based, using the UUIDv7 of the
  last seen note (`?after=<uuid>&limit=50`).
- **`PATCH /notes/{id}` for content edits.** The `@Version` field is
  already in place to surface 409 Conflict on concurrent edits.
- **Audit log emission to a message broker** so downstream consumers can
  subscribe to note lifecycle events without polling the database.
- **Rate limiting** at the gateway tier.
