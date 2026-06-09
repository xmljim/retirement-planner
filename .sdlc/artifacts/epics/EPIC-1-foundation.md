# EPIC-1: Foundation & Plan Aggregate

**Milestone**: M1 — Foundation
**Total points**: ~27
**Goal**: A user can create a Plan with two People and several Accounts (with sleeves) via REST and re-fetch them. Modulith verification passes in CI.

## Stories

### S-1.1 — Bootstrap Spring Boot project with Modulith
**As a** developer **I want** a runnable Spring Boot 4.x project with Spring Modulith configured **so that** module boundary verification works from day one.

**Acceptance criteria**
- `./mvnw spring-boot:run` starts the app on a configured port
- Module packages exist per ADR-008: `plan/`, `contribution/`, `tax/`, `bucket/`, `allocation/`, `returns/`, `simulation/`, `scenario/`, `api/`, `shared/`
- `ApplicationModulesIntegrationTest` runs `ApplicationModules.verify()` and passes
- `application.yml` configured with `local` and `prod` profiles
- Spotless / Checkstyle / PMD configured per `quality` skill

**Points**: 3
**Traces to**: ADR-001, ADR-008, NFR-9

---

### S-1.2 — Configure Postgres + Flyway
**As a** developer **I want** PostgreSQL accessible via Podman compose with Flyway migrations **so that** schema is versioned from the start.

**Acceptance criteria**
- `compose.yaml` defines a Postgres service runnable via `podman compose up`
- Flyway baseline migration `V1__init.sql` creates a `tenants` table with one `solo` row
- App starts and connects in `local` profile
- README includes Podman vs Docker note (no Docker requirement)

**Points**: 2
**Traces to**: ADR-001, ADR-006

---

### S-1.3 — Implement Money value type
**As an** engine **I want** a `Money` value record with arithmetic helpers **so that** all monetary code uses one type.

**Acceptance criteria**
- `shared/Money.java` is a record with `BigDecimal amount`, `Currency currency`
- Static factories: `usd(String)`, `of(BigDecimal, Currency)`, constants `ZERO_USD`
- Arithmetic: `plus`, `minus`, `times(BigDecimal)`, `dividedBy(BigDecimal)`, `negate`
- Cross-currency operations throw
- Internal scale 6, `HALF_EVEN` rounding
- `MoneyDisplay.toDisplay(Money)` rounds to scale 2 for UI use
- ≥ 95% line coverage; tests cover precision over many additions, rounding edge cases

**Points**: 3
**Traces to**: ADR-007

---

### S-1.4 — Add Checkstyle rule banning `double` outside the MC inner loop
**As a** maintainer **I want** Checkstyle to fail the build when `double` or `float` appear outside `simulation.montecarlo.internal` **so that** the BigDecimal contract is mechanically enforced.

**Acceptance criteria**
- Checkstyle config rejects `double`/`float` in any package except `simulation.montecarlo.internal.*`
- A deliberately-failing test PR demonstrates the rule fires
- `quality` skill picks it up

**Points**: 2
**Traces to**: ADR-007

---

### S-1.5 — Define Plan, Household, Person aggregates
**As a** user **I want** to create a Plan with a Household and 1–2 People **so that** my demographic context exists.

**Acceptance criteria**
- JPA entities `Plan`, `Household`, `Person` in `plan/internal/`
- `Plan` carries `tenantId` (per ADR-002 multi-tenancy)
- `Person` carries DOB and references to `SalaryProfile` (entity, contents may be empty for now)
- `Household` carries `FilingStatus` (enum) and `state` (string)
- Flyway `V2__plan_household_person.sql` creates schema
- Repository interfaces in `plan/` (public): `PlanRepository`, with `tenantId` filtered automatically
- Unit tests cover happy-path persistence

**Points**: 5
**Traces to**: FR-1.1, FR-1.2, FR-1.3, ADR-002

---

### S-1.6 — Define Account and AccountSleeve
**As a** user **I want** to define accounts with one or more sleeves **so that** the model can represent cash sleeves in tax-advantaged accounts.

**Acceptance criteria**
- `Account` entity with `AccountType` enum (Trad 401k, Roth 401k, Trad IRA, Roth IRA, HSA, Taxable, Cash, Pension)
- `AccountSleeve` entity with `SleeveKind` sealed type (Cash, AssetAllocation, FixedAllocation), `Money balance`, `SleeveYieldPolicy`
- New account defaults to a single `AssetAllocation` sleeve holding the full balance
- Owner reference (Person or Joint) modeled
- Flyway `V3__account_sleeve.sql` creates schema with `NUMERIC(19,6)` for money columns per ADR-007
- Repository tests confirm round-trip including sleeve list

**Points**: 5
**Traces to**: FR-2.1, FR-2.2, FR-2.3, ADR-002, ADR-007

---

### S-1.7 — REST endpoints for Plan / Person / Account
**As a** user **I want** REST endpoints to create, fetch, and list Plans, People, and Accounts **so that** I can drive the domain via API.

**Acceptance criteria**
- `api/` controllers delegate to `plan/` services (no business logic in controllers per CLAUDE.md)
- Endpoints: POST/GET/PUT/DELETE for Plan, Person, Account; GET for sleeves under an account
- Request/Response DTOs in `api/dto/` — Money serialized as `{"amount":"...","currency":"USD"}` per ADR-007
- OpenAPI spec generated; checked into the repo at build time
- Integration tests cover happy paths and 404s

**Points**: 5
**Traces to**: FR-1.1, FR-2.1, ADR-001, NFR-9

---

### S-1.8 — Stub authentication for solo mode
**As a** developer **I want** a stub `AuthenticationProvider` that injects an "owner" principal **so that** tests and dev mode work without real auth.

**Acceptance criteria**
- `app.auth.mode` config flag with values `stub | passkey`
- `stub` mode wires a single `owner` principal with a fixed `tenantId`
- `passkey` mode wires the WebAuthn provider (skeleton only — passkey integration tests deferred to M8)
- Default for `local` profile is `stub`; `prod` requires explicit setting

**Points**: 2
**Traces to**: FR-10.1, FR-10.2, ADR-001
