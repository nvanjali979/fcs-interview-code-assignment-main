# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes. The codebase uses two different patterns in parallel:

- Store and Product use the Panache Active Record pattern (PanacheEntity): the entity class itself
  carries findById, persist, delete, etc. This is quick to write but couples persistence logic
  directly to the domain entity and makes unit-testing without a database harder.

- Warehouse uses the Panache Repository pattern (PanacheRepository<DbWarehouse>) with a clean
  separation between the JPA entity (DbWarehouse) and the domain model (Warehouse), backed by
  port interfaces (WarehouseStore, LocationResolver). This follows hexagonal / ports-and-adapters
  architecture and is significantly easier to unit-test and evolve independently.

I would migrate Store and Product to the repository pattern for the following reasons:

1. Consistency – one strategy across the codebase reduces cognitive overhead for anyone joining
   the project.
2. Testability – domain logic (use cases) can be tested with mock/in-memory implementations of the
   port interfaces, no database container required for unit tests.
3. Separation of concerns – changing the persistence technology or schema does not force changes to
   business logic, and vice-versa.
4. Easier extension – when Store or Product need richer domain behaviour (e.g. the BONUS task of
   associating Warehouses with Products/Stores), keeping that logic in a use case is cleaner than
   adding it to a PanacheEntity.

The migration cost is low for these two simple entities, and the architectural consistency gained
is worth it.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-first (generate code from YAML spec)
  Pros:
  - The contract is explicit and version-controlled; any breaking change is immediately visible
    in a diff of the spec file.
  - Documentation, client SDKs, server stubs, and validation schemas can all be derived from a
    single source of truth.
  - Enables parallel development: frontend/consumer teams can start coding against the spec before
    the server implementation exists (mocking servers, Prism, etc.).
  - Makes API governance easier – contract reviews happen before any code is written.
  Cons:
  - Extra tooling in the build pipeline (generator, regeneration triggers); can produce verbose or
    unwieldy generated code that developers do not fully control.
  - When the generator output changes (version bumps), unexpected diffs appear in committed stubs.
  - Slight friction for small, internal-only APIs: the roundtrip of editing YAML then regenerating
    is slower than writing a resource class directly.

Code-first (write JAX-RS handlers directly, optionally export spec via annotation scanning)
  Pros:
  - Faster initial implementation; no intermediate artifact.
  - Full control over code structure; no generated boilerplate.
  - Natural fit for APIs that are purely internal and consumed by the same team.
  Cons:
  - The contract is implicit; it lives inside the implementation and drifts if not carefully
    maintained.
  - Consumer teams must either read the source or rely on generated documentation that can lag.
  - Harder to enforce consistency across multiple services owned by different teams.

My choice:
For any API that crosses a team or system boundary – including the Warehouse API, which integrates
with external consumers and a legacy system – I would choose OpenAPI-first. The investment in
maintaining the YAML spec pays off in contract clarity, automated validation, and parallel
development velocity.

For small, purely internal helper endpoints owned by a single team, code-first with auto-generated
documentation (e.g. quarkus-smallrye-openapi scanning annotations) is a pragmatic shortcut. That
said, I would align both approaches under the same generator pipeline over time to keep the
codebase consistent, as we discussed in Question 1.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order (highest ROI first):

1. Domain use-case unit tests (fast, no infrastructure)
   The use cases – CreateWarehouseUseCase, ReplaceWarehouseUseCase, ArchiveWarehouseUseCase –
   contain all the business rules (BU code uniqueness, location validity, capacity constraints,
   stock matching). These are pure Java classes whose dependencies are interfaces (WarehouseStore,
   LocationResolver); they can be exercised with Mockito or a simple in-memory stub. These tests
   run in milliseconds, give precise failure messages, and protect the most critical logic.
   I would cover every validation path and every happy path here.

2. Repository integration tests (Quarkus @QuarkusTest + in-memory H2 / Testcontainers)
   Verify that WarehouseRepository queries work correctly: findByBusinessUnitCode returns only
   active records, getAll excludes archived warehouses, update sets archivedAt correctly. These
   tests are slower but small in number – one per repository method is usually enough.

3. API / end-to-end integration tests (@QuarkusIntegrationTest with RestAssured)
   Cover the critical user journeys: create → list → archive → list (warehouse gone), and the
   replace flow. These tests validate wiring (CDI injection, transaction boundaries, HTTP status
   codes). Keep them to the golden path plus the most important error cases (404, 409, 400).
   They are expensive to run, so a small focused suite is better than exhaustive coverage here.

4. Gateway / adapter contract tests
   A smoke test for LocationGateway (already stubbed in LocationGatewayTest) to confirm all
   expected locations resolve correctly and unknown identifiers return null.

Keeping coverage effective over time:
- Enforce that every new business rule ships with a use-case unit test (PR review checklist).
- Run unit tests on every commit, integration tests on every PR merge to main (CI gating).
- Prefer testing behaviour (given this input, expect this outcome) over testing implementation
  details (verify this mock was called); the former survives refactors.
- Track coverage as a trend, not an absolute target – a drop in coverage on a PR signals a gap
  worth investigating, but a static 80% gate can give false confidence.
```