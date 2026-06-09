# EPIC-6: Scenario Management

**Milestone**: M6 — Scenario Management
**Total points**: ~17
**Goal**: User can save, clone, run, compare, and delete scenarios via API. Snapshots persist to BlobStore. Run results cache by inputsHash + seed.

## Stories

### S-6.1 — Scenario entity and CRUD
**As a** user **I want** to save a Scenario with a name and description tied to a Plan **so that** I can iterate on scenarios.

**Acceptance criteria**
- `Scenario` JPA entity in `scenario/internal/`: id, name, description, planId, runConfig (embedded), latestRunId
- Repository, service, controller delegating per CLAUDE.md
- REST: POST/GET/PUT/DELETE for scenarios
- Tests: round-trip; constraint: scenario must reference a Plan in same tenant

**Points**: 3
**Traces to**: FR-8.1, FR-8.5, ADR-006

---

### S-6.2 — Snapshot writer with inputsHash
**As an** engine **I want** every save to write a Parquet snapshot with a canonical inputsHash **so that** runs can be cached and history is auditable.

**Acceptance criteria**
- On save, if scenario inputs differ from latest snapshot: write a new versioned snapshot Parquet
- `inputsHash` = SHA-256 of canonicalized inputs (sorted keys, normalized BigDecimals)
- Canonicalizer is a tested utility — same inputs in any order produce same hash; different inputs produce different hash
- Snapshot includes Plan + Scenario + RunConfiguration (per ADR-006)
- Path: `tenants/{tenantId}/scenarios/{scenarioId}/snapshots/v{n}-{instant}.parquet`
- Tests: hash stability across key orderings, snapshot read-back round-trip

**Points**: 5
**Traces to**: FR-8.6, ADR-006

---

### S-6.3 — Run result cache
**As a** user **I want** repeat runs of identical inputs to return cached results **so that** I don't wait 5s for the same answer twice.

**Acceptance criteria**
- Cache key: `(inputsHash, seed)`; cache value: `runId`
- Cache table in Postgres (or simple Parquet index) — choice documented in design
- On run request: lookup → if hit, return existing runId; if miss, execute and store
- Cache invalidation on scenario delete
- Tests: hit, miss, hit after seed change, hit after scenario edit (no hit when inputs change)

**Points**: 3
**Traces to**: FR-8.3, ADR-006

---

### S-6.4 — Scenario clone
**As a** user **I want** to clone a scenario **so that** I can tweak the copy independently.

**Acceptance criteria**
- `POST /scenarios/{id}/clone` creates a new scenario referencing a deep-copied Plan
- New scenario has fresh id, prepended name "Copy of …", same RunConfiguration
- Snapshot lineage preserved: new snapshot's `parentVersion` points to source's latest version
- Tests: clone, edit clone, run both, confirm independence

**Points**: 3
**Traces to**: FR-8.2

---

### S-6.5 — Scenario comparison endpoint
**As a** user **I want** to compare 2+ scenarios on the same axes **so that** I can evaluate trade-offs side by side.

**Acceptance criteria**
- `GET /scenarios/compare?ids=A,B,C&metric=portfolioValue|successProbability|bucketSpend`
- Returns aligned time-series per scenario for the requested metric
- Each scenario must have at least one completed run; otherwise 409 Conflict with a clear message
- Tests: 2-scenario, 3-scenario; mismatched horizons (shorter scenario padded with null)

**Points**: 3
**Traces to**: FR-8.4, FR-9.4

---

### S-6.6 — Modulith events for scenario lifecycle
**As a** developer **I want** Modulith events emitted on scenario save / run-completed / delete **so that** snapshot writing and cache invalidation are decoupled.

**Acceptance criteria**
- Events: `ScenarioSavedEvent`, `RunCompletedEvent`, `ScenarioDeletedEvent` published via `ApplicationEventPublisher`
- `@ApplicationModuleListener` handlers in `scenario/internal/`: snapshot writer, cache invalidator
- Modulith outbox semantics verified by an integration test (handler doesn't run if publisher's transaction rolls back)
- These are the only events introduced — hot-path inter-module calls remain direct per CLAUDE.md

**Points**: 3
**Traces to**: ADR-008, NFR-12
