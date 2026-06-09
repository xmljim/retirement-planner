# EPIC-7: API & Frontend Contract

**Milestone**: M7 — Frontend Integration (parallel)
**Total points**: ~11
**Goal**: A stable, documented API contract that the `retirement-planner-ui` repo can integrate against. OpenAPI spec is the source of truth.

## Stories

### S-7.1 — OpenAPI generation in CI
**As a** frontend developer **I want** an up-to-date OpenAPI spec generated on every backend build **so that** frontend types stay in sync.

**Acceptance criteria**
- springdoc-openapi configured; spec generated at `target/openapi.json` on build
- CI publishes the spec as a build artifact
- A check fails the build if controllers introduce undocumented endpoints or DTOs
- README documents how to consume the spec

**Points**: 2
**Traces to**: NFR-12 (loosely; spec-first contract)

---

### S-7.2 — Money DTO serialization contract
**As a** frontend developer **I want** Money serialized as `{"amount":"...","currency":"USD"}` with amount as a string **so that** JS Number precision loss can't happen.

**Acceptance criteria**
- Custom Jackson serializer/deserializer for Money
- All endpoints exposing Money use this format
- OpenAPI spec documents the format with an example
- Tests: round-trip JSON, large-number precision (e.g. $1,234,567.890123)

**Points**: 2
**Traces to**: ADR-007

---

### S-7.3 — Error response contract
**As a** frontend developer **I want** consistent error responses across all endpoints **so that** error handling is uniform.

**Acceptance criteria**
- `@ControllerAdvice` global handler producing RFC 7807 (problem+json) responses
- Domain validation errors (e.g. invalid bucket configuration) map to 400 with field-level details
- 404 for missing resources, 409 for conflict (e.g. duplicate scenario name in tenant)
- Stack traces never leaked in non-dev profiles
- Tests cover the three error categories above

**Points**: 3
**Traces to**: NFR-9

---

### S-7.4 — Pagination and filtering for list endpoints
**As a** frontend developer **I want** consistent pagination on list endpoints **so that** large data sets render incrementally.

**Acceptance criteria**
- Page-based pagination with `page`, `size`, `sort` query params on all list endpoints
- Response envelope: `{ data: [...], page, size, totalElements, totalPages }`
- OpenAPI documents the pattern
- Tests: empty list, single page, multi-page, sort-by-name vs sort-by-created

**Points**: 2
**Traces to**: NFR-9

---

### S-7.5 — Frontend-align session
**As a** developer **I want** to formally align the API contract with the frontend repo's expectations **so that** no missing endpoint emerges late.

**Acceptance criteria**
- Run the `frontend-align` skill to compare what the UI needs vs. what the API exposes
- Each gap captured as a follow-up story added to the relevant epic
- Result documented in `docs/architecture/api-contract-review.md`

**Points**: 2
**Traces to**: process / coordination
