# Claude Code Instructions

## Project Overview

Retirement Planner is a Spring Boot application that produces month-by-month
retirement projections across the **accumulation**, **bridge**, and
**drawdown** phases. It supports goal-based **buckets** (Bridge, Travel,
Bucket-list, Healthcare, Legacy) with adaptive spending policies, full
**tax modeling** (federal + state, RMDs, Roth conversions, taxable SS,
SECURE 2.0 §603/§604), and **Monte Carlo** simulation with glide-path
asset allocation and historical block bootstrap.

The frontend (`retirement-planner-ui`) is a separate React/TypeScript repo.

## Repository

- This repo: `retirement-planner` (Java/Spring Boot backend)
- Frontend (future): `retirement-planner-ui` (React/TypeScript)

## Before Starting Work (MANDATORY)

Familiarize yourself with the project before doing any work. Required reading:

1. **Discovery & PRD**
   - `.sdlc/artifacts/discovery/DISC-001-20260608.yaml` — problem framing, scope, success criteria
   - `.sdlc/artifacts/prd/PRD-001-retirement-planner.md` — functional & non-functional requirements, milestones

2. **Architectural Decision Records** (`.sdlc/artifacts/adr/`)
   - **ADR-001** Platform & Infrastructure (repo topology, passkeys, Podman, deployment posture)
   - **ADR-002** Domain Model (Plan, Person, Account/Sleeve, Bucket, FundingPolicy, SpendingPolicy, LifecyclePolicy, reallocation)
   - **ADR-003** Accumulation & Contribution Model (IRS limits hierarchy, employer match, SECURE 2.0 §603/§604)
   - **ADR-004** Tax Engine (federal+state, RMDs, Roth conversions, taxable SS)
   - **ADR-005** Monte Carlo & Historical Returns (block bootstrap, glide path, dual precision)
   - **ADR-006** Scenario Persistence & Versioning (Postgres + Parquet via BlobStore)
   - **ADR-007** Money Representation & Precision (BigDecimal, scale 6/2, HALF_EVEN)
   - **ADR-008** Module Boundaries with Spring Modulith
   - **ADR-009** Quality Gates — Toolchain & Enforcement
   - **ADR-010** Branching Strategy — Trunk-Based Development

3. **Session Context**
   - `.sdlc/context/session-context.yaml` — current SDLC phase and active artifacts

ADRs are **contracts**. If an issue conflicts with an ADR, stop and ask
before deviating — do not silently change the contract. The "30-second
rule" applies: a question now prevents hours of rework.

### Sub-Agent Instructions (MANDATORY)

Sub-agents spawned via the Agent tool do NOT inherit CLAUDE.md context.
When delegating, include in the prompt:

> Before doing any work, read /Users/jearley/code/retirement-planner/CLAUDE.md
> and follow the "Before Starting Work" section. Pay particular attention to
> the relevant ADRs (.sdlc/artifacts/adr/) for the area you're working in.

## Design Philosophy

**Declarative over imperative. Composition over mutation.**

- `Optional<T>` for absent values; `null` reserved for JSON DTOs only
- Stream API over `for(item : iterable)` loops
- Sealed interfaces for value-like polymorphism (Bucket, FundingPolicy, SpendingPolicy, LifecyclePolicy)
- Records for value types; immutable by default
- Constructor injection; no field injection, no setter injection
- Controllers delegate, never compute — business logic lives in services

These aren't style preferences. They reduce a class of bugs and make
boundary violations visible.

## Code Standards

- **Java**: 25 (per ADR-008 / current Spring Boot baseline)
- **Spring Boot**: 4.x with Spring Modulith
- **Build**: Maven; `./mvnw` wrapper

### Money & Precision (ADR-007)

- All monetary values use the `Money` value record (BigDecimal-backed)
- Internal scale: 6; display scale: 2; rounding: `HALF_EVEN`
- Rates are `BigDecimal` decimals, not percentages (0.0245, not 2.45)
- **`double` is forbidden outside the Monte Carlo inner loop** in
  `simulation.montecarlo.internal`. A Checkstyle rule enforces this.

### Null Safety

```java
// BAD
public User findByEmail(String email) {
    return repository.findByEmail(email); // could be null
}

// GOOD
public Optional<User> findByEmail(String email) {
    return repository.findByEmail(email);
}
```

Exception: DTOs used for JSON serialization may use `null` since Jackson
handles it naturally.

### Iteration

```java
// BAD
for (Account a : accounts) {
    a.process();
}

// GOOD
accounts.stream().forEach(Account::process);
```

### Imports — no fully-qualified names

```java
// BAD
public void check() {
    org.assertj.core.api.Assertions.assertThat(x).isEqualTo(1);
}

// GOOD
import static org.assertj.core.api.Assertions.assertThat;

public void check() {
    assertThat(x).isEqualTo(1);
}
```

Imports are the single source of truth for a file's dependencies. Don't
scatter that information into the method body via FQNs. Suppress only
when there's a true name collision that can't be resolved otherwise.

### Timestamps

All timestamps stored and transmitted in UTC (`Instant.now()`, not
`LocalDateTime.now()`). The frontend handles local-time conversion.

### Controller Pattern

Controllers receive the request, delegate to a service, return the
result. No validation, parsing, or business logic in controllers.

### Package Structure

Per ADR-008, top-level packages are Modulith modules:

```
io.github.xmljim.retirement.retirementplanner
├── plan/                     # Plan, Household, Person, Account, Sleeve aggregates
├── contribution/             # Accumulation engine, IRS limits, §603/§604
├── tax/                      # TaxEngine, brackets, RMDs, conversions
├── bucket/                   # Bucket engine (Bucket value types live in plan/)
├── allocation/               # Glide path, asset classes
├── returns/                  # Historical returns dataset access
├── simulation/               # Orchestrator, Monte Carlo
├── scenario/                 # Scenario, Snapshot, run caching
├── api/                      # Controllers (delegating only)
└── shared/                   # Money, CashFlow, cross-cutting value types
```

Within each module:

```
<module>/
├── *Engine.java | *Service.java   # public API
├── *.java                          # public value records / interfaces
└── internal/                       # private to the module — others may not import
```

## Spring Modulith — Hot-Path / Cold-Path Rule (ADR-008)

**Critical rule. Read this before adding any cross-module call.**

| Path | When | How |
|---|---|---|
| **Hot path** | Per-month or per-year simulation calls; synchronous data dependencies (e.g. tax engine asking for W-2 wages) | **Direct method calls on injected interfaces.** Plain Spring DI. |
| **Cold path** | User-initiated workflow boundaries (scenario saved, run completed, scenario deleted, account added) | **`@ApplicationModuleListener` events.** Modulith outbox is on by default. |

**Do not reach for `@ApplicationModuleListener` because "events are decoupled and that sounds good."** The Monte Carlo loop runs millions of cross-module calls per request; routing those through Spring's event bus blows the 5-second performance budget by orders of magnitude.

If you can answer "yes" to all three, it's hot-path → direct call:
1. Is this called inside a per-month or per-year loop?
2. Does the caller need the result to continue?
3. Is the call synchronous?

If you can answer "yes" to all three, it's cold-path → event:
1. Is the trigger human-initiated (save, delete, run)?
2. Can handlers be eventually-consistent?
3. Could ≥2 modules want to react without the publisher knowing?

### Module Boundary Enforcement

`ApplicationModules.verify()` runs as a unit test in CI and fails the
build on illegal cross-module access (e.g. `simulation/` reaching into
`tax/internal/`). If verification fails, **fix the dependency** rather
than weakening the boundary.

Promoting something from `internal/` to public API is a deliberate
decision — only do it when another module proves a need.

## Skill Usage

This project has access to a set of skills (see your skill list). Use
them rather than duplicating their workflows here.

**Code-quality and review:**
- `/quality fix` — run formatters and linters before committing (REQUIRED before commit)
- `/java-review` — Java pattern check
- `/code-review` — review the current diff
- `/security-review` — security review
- `/simplify` — apply readability/simplification fixes

**Testing:**
- `/unit-test` — generate unit tests
- `/integration-test` — generate integration tests
- `/test` — run the test suite

**Component creation (use as templates):**
- `/java-service` — new service classes
- `/java-controller` — new REST controllers
- `/java-entity` — new JPA entities
- `/migration` — Flyway migration

**API surface:**
- `/api-change` — document an API change after modifying endpoints/DTOs
- `/openapi` — OpenAPI spec work
- `/api-types` — generate types from the API
- `/frontend-align` — coordinate frontend/backend before cross-cutting work

**Workflow:**
- `/issue` — start work on a GitHub issue
- `/pr` — create a pull request
- `/pr-comments` — review and address PR comments
- `/release` — release process
- `/commit` — create a git commit
- `/commit-push-pr` — commit, push, and open a PR
- `/session-end` — update WORKING_CONTEXT.md at end of session

**Documentation:**
- `/javadoc` — generate or update Javadoc

**SDLC artifacts (this project's process):**
- `/sdlc-discover` — start a discovery
- `/sdlc-define` — produce a PRD from discovery
- `/sdlc-artifact` — generate an SDLC artifact
- `/sdlc-status` — show current SDLC state

**Project-local skills:**
- `/retirement-style` — project-specific style and architecture review (Money, Modulith hot/cold path, sealed interfaces, SaaS-readiness, NFR-8 disclaimer). Run before commit alongside `/quality`.

**Verify changes:**
- `/run` — launch the app to see a change in action
- `/verify` — verify a fix or PR works

## Development Workflow (Trunk-Based per ADR-010)

`main` is the only long-lived branch. All work happens on short-lived
`feature/*` branches and merges back via PR. Project board status moves
are automated — see ADR-011 and `.github/workflows/project-automation.yml`:

- Push `feature/issue-NN-*` → issue auto-moves to **In Progress**
- Open PR with `Closes #NN` → issue auto-moves to **In Review**
- Merge PR → issue auto-moves to **Done** and closes

Per-story workflow:

1. Pick a story from the Project board's "Ready" column
2. Read its linked Epic / PRD / ADR references in the issue
3. Branch: `git checkout -b feature/issue-NN-short-description` from `main`
4. Implement with tests
5. Run quality gates locally — `./mvnw verify` must pass
6. Run `/quality fix`, `/java-review`, `/retirement-style` before commit
7. Commit referencing the issue: `git commit -m "Add X (#NN)"`
8. Push and `gh pr create --base main` with `Closes #NN` in the body
9. CI runs all 9 status checks — they must pass before merge
10. `gh pr merge --squash --delete-branch` after CI green
11. Run `/session-end` at natural breakpoints (see Memory Hygiene)

For larger changes, present a plan and wait for approval before implementing.

## Quality Gates (ADR-009)

**Every PR must pass `./mvnw verify` before merge.** That single command
runs:

| Gate | Tool | Phase |
|---|---|---|
| Format | Spotless (palantir-java-format) | validate |
| Style | Checkstyle | validate |
| Code smells | PMD | verify |
| Bytecode bugs | SpotBugs + findsecbugs | verify |
| Architecture | ArchUnit (in tests) | test |
| Module boundaries | Spring Modulith | test |
| Coverage | JaCoCo (engine 85/75, domain 75/65, total 70/60) | verify |
| Tests | Surefire (unit) + Failsafe (integration) | test / verify |

**Strict severity** — any violation fails the build. CI runs the same
suite on every PR via `.github/workflows/ci.yml`. Branch protection on
`main` blocks merge until all status checks pass.

**Before commit**, invoke skills in this order:

1. `/quality fix` — auto-format and run linters (catches most style issues)
2. `/retirement-style` — project-specific judgment review (Money, Modulith hot/cold path, sealed interfaces, SaaS-readiness)
3. `/java-review` — generic Java pattern check
4. `/test` — run the test suite
5. `/code-review` — final diff review

If any gate flags something legitimate that requires a suppression:
narrowest-possible scope, justification comment required, mention in PR.

## Memory Hygiene

Run `/session-end` at natural breakpoints — typically:

- **After a story merges** to `main` (the per-story save point)
- **At the end of a working session**, even if no story merged
- **After a substantive decision** that future sessions need to know about (a new ADR, a scope change, a new convention)

`/session-end` updates `WORKING_CONTEXT.md` and the memory index so the
next session inherits accurate state. Stale memory is worse than no
memory — it leads future sessions to act on outdated facts.

When picking up a session, the inverse habit: **read `WORKING_CONTEXT.md`
and `.sdlc/context/session-context.yaml` first** before starting work.

## Releases (ADR-010)

A release is a tag on `main`, not a branch. Use the `/release` skill.
There is no `develop`, no `release/*`, no `hotfix/*`. Patches off a tag
when needed (rare).

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=ClassName

# Coverage
./mvnw test jacoco:report
```

Engine modules (contribution, tax, simulation, bucket) require ≥ 80%
line coverage per NFR-10. Reference fixtures must replicate the input
spreadsheet (`~/planner.xlsx` Sheet2) to within 1% on matched inputs.

## Building

```bash
./mvnw compile
./mvnw package -DskipTests
./mvnw spring-boot:run
```

## Local Development (Podman)

Per ADR-001, this project uses **Podman**, not Docker.

```bash
podman compose up -d        # start Postgres
podman compose down         # stop
```

Dev profile (`local`) uses the Podman Postgres; `prod` profile is
externally configured.

## License & AI Use

This project is licensed **PolyForm Noncommercial 1.0.0** with an addendum
that prohibits use of the source as AI / ML training data
(`LICENSE-ADDENDUM.md` §1). When generating, modifying, or summarizing
this codebase:

- **Do not** copy substantial portions verbatim into external systems
  whose purpose is training models.
- **Do not** embed the source into vector stores or RAG corpora that feed
  output to AI systems beyond this user's own working session.
- Reading, transforming, and editing the code as part of an interactive
  Claude Code session is the licensed and intended use; bulk export for
  training is not.

When generating user-facing copy, error messages, or new code, include a
copyright header on new source files:

```java
/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
```

(Spotless will normalize formatting; the header itself is project-mandated
content that should not be removed by automation.)

## Output is Not Financial Advice

Per NFR-8, the application includes a per-page disclaimer that output
is illustrative and not financial advice. When generating user-facing
copy, charts, or reports, preserve this disclaimer. Do not generate
language that reads as a recommendation ("you should retire at 62").
Frame outputs as projections and trade-offs.

## Key Documents Index

| Document | Path |
|---|---|
| Discovery | `.sdlc/artifacts/discovery/DISC-001-20260608.yaml` |
| PRD | `.sdlc/artifacts/prd/PRD-001-retirement-planner.md` |
| ADR Index | `.sdlc/artifacts/adr/README.md` |
| Session Context | `.sdlc/context/session-context.yaml` |
| Spreadsheet reference | `~/planner.xlsx` (Sheet2) |
