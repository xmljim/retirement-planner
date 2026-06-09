# ADR-009: Quality Gates — Toolchain & Enforcement

- **Status**: Accepted
- **Date**: 2026-06-08
- **Deciders**: Owner
- **Related**: ADR-001 (platform), ADR-007 (Money), ADR-008 (Modulith), ADR-010 (TBD branching)

## Context

Per the owner's mandate, every PR must pass a strict quality bar
before merge. The bar must be mechanical (no humans-in-the-loop for
basic style), tailored to the conventions captured across ADR-002
through ADR-008, and uniform across all stories.

Manual code review is a backstop for *judgment* questions (architecture,
naming, edge-case handling), not a primary enforcement mechanism for
*style* questions. Style and structure are CI's job.

## Decision

### Tools (all bound to `mvn verify`, all fail-on-violation)

| Tool | Purpose | Maven phase |
|---|---|---|
| **Spotless** | Code formatting (format on save + diff check in CI) | `validate` |
| **Checkstyle** | Style rules: naming, imports, brace placement, project-specific rules | `validate` |
| **PMD** | Code-smell rules: unused params, complexity, common bugs | `verify` |
| **SpotBugs** | Bytecode analysis: null deref, resource leaks, concurrency bugs | `verify` |
| **ArchUnit** | Architectural rules expressed as tests | `test` |
| **JaCoCo** | Coverage measurement + threshold enforcement | `verify` |
| **Spring Modulith verify** | Module boundary enforcement (ADR-008) | `test` |

A failing run for **any** of these fails `mvn verify`. CI runs
`mvn verify` on every PR; merge is blocked on failure (per ADR-010
branch protection).

### Severity: Strict

Per the owner's directive: **fail the build on any violation.** No
warning-only mode. If a finding is a true false-positive, it gets a
narrow, justified suppression with a comment explaining why; the
linked PR review notes the suppression for visibility.

### Coverage Thresholds

Per-module thresholds (configured in `pom.xml` per child or via Maven
profiles):

| Module category | Modules | Line | Branch |
|---|---|---|---|
| **Engine** | `contribution`, `tax`, `bucket`, `simulation`, `allocation` | **85%** | **75%** |
| **Domain** | `plan`, `scenario` | 75% | 65% |
| **Infrastructure** | `returns`, `shared` | 75% | 65% |
| **API surface** | `api` | 60% | 50% |
| **Project total** | (overall, after exclusions) | **70%** | **60%** |

Exclusions (don't count toward coverage):
- Generated sources (OpenAPI types, MapStruct, etc.)
- Spring configuration classes annotated `@Configuration` with no logic
- DTOs (records) — covered transitively by integration tests
- `Application.java` main class

A coverage shortfall fails the build with a clear message naming the
module that missed.

### Project-Specific Rules

These are the rules unique to this project, codified in tool config so
they never depend on review attention:

#### `double` / `float` ban (ADR-007)
- **Tool**: Checkstyle (custom regex rule)
- **Scope**: All packages **except** `simulation.montecarlo.internal.*`
- **Failure message**: "Forbidden primitive: use `Money` per ADR-007. The Monte Carlo inner loop is the only sanctioned exception (ADR-005, ADR-007)."

#### Optional<T> for absent values (ADR-002, design philosophy)
- **Tool**: PMD (custom rule based on `OptionalGetWithoutIsPresent` etc.)
- **Scope**: All non-DTO packages
- **Rules**: No method returning `null` for "not found"; use `Optional`. DTOs (anything in `**/dto/**` or `**/api/dto/**`) are exempt.

#### No `for(item : iterable)` (CLAUDE.md)
- **Tool**: PMD (custom rule)
- **Scope**: All packages
- **Failure message**: "Use Stream API per CLAUDE.md. Enhanced-for loops are forbidden in this codebase."
- **Exception**: PMD-suppression with comment when the body has side effects that don't compose well as a stream (rare; expect ≤ 5 in the codebase).

#### Controllers delegate only (CLAUDE.md, ADR-001)
- **Tool**: ArchUnit
- **Rule**: classes annotated `@RestController` or `@Controller` may only:
  - inject service-layer interfaces (constructor injection)
  - call methods on those services and return the result
  - throw exceptions handled by the global advice (S-7.3)
  - **not** import or call any class in `**.internal.*`, any repository, any entity directly

#### Modulith boundaries (ADR-008)
- **Tool**: Spring Modulith `ApplicationModules.verify()`
- **Rule**: Cross-module access only via the public package of each module. `internal/` is private.

#### No event-bus in hot path (ADR-008, CLAUDE.md)
- **Tool**: ArchUnit
- **Rule**: classes in `**.simulation.**` (and their internal packages) may not import `ApplicationEventPublisher` or use `@ApplicationModuleListener`.

#### UTC timestamps only (CLAUDE.md)
- **Tool**: ArchUnit + PMD
- **Rule**: `LocalDateTime.now()` and `new Date()` (zero-arg) banned. Use `Instant.now()`. `LocalDate.now()` permitted with a clock-injection pattern.

#### Constructor injection only (CLAUDE.md)
- **Tool**: ArchUnit
- **Rule**: `@Autowired` on fields and setters is banned. Records or `final` fields with a single explicit constructor are required.

#### BigDecimal direct construction from double
- **Tool**: PMD (`AvoidDecimalLiteralsInBigDecimalConstructor`)
- **Rule**: `new BigDecimal(double)` banned. Use `BigDecimal.valueOf(double)` or, preferably, `new BigDecimal(String)` per ADR-007.

#### Fully-qualified class names in code bodies
- **Tool**: PMD (custom XPath rule `NoFullyQualifiedNames`)
- **Rule**: References to classes by their fully-qualified name (e.g. `org.assertj.core.api.Assertions.assertThat(...)`) are banned. Add an import.
- **Rationale**: Imports are the single source of truth for a file's dependencies. FQNs scatter that information through the body and obscure it.
- **Exception**: True import collisions (rare) — suppress narrowly with `@SuppressWarnings("PMD.NoFullyQualifiedNames")` and a comment.

### Skill Coverage

The existing `/quality` and `/java-review` skills run the toolchain and
flag violations against PlaceFinder conventions. Most apply directly;
the project-specific rules above are encoded in the tool configs and
will surface naturally.

A small project-local skill, `/retirement-style`, captures the
project-specific guidance that won't be obvious to a sub-agent reading
PlaceFinder skills:

- The `Money` value type contract
- The Modulith hot-path / cold-path rule
- Sealed interface patterns for Bucket / FundingPolicy / SpendingPolicy
- Where each kind of code goes (engine in `<module>/`, types in `plan/`)

The `/retirement-style` skill is invoked before commit alongside
`/quality`. CLAUDE.md is updated to reference it.

### Local-vs-CI Parity

`./mvnw verify` runs the **full** suite locally — same as CI. There is
no "fast" local profile that skips checks; if CI catches it, local
should catch it. (Spotless's `apply` goal applies fixes; the `check`
goal in `verify` confirms cleanness.)

For inner-loop iteration during development, individual tool goals are
available:
- `./mvnw spotless:apply` — auto-format
- `./mvnw checkstyle:check`
- `./mvnw pmd:check`
- `./mvnw spotbugs:check`
- `./mvnw test -Dtest=*ArchitectureTest`
- `./mvnw test jacoco:report` — coverage report at `target/site/jacoco/`

### Suppressions Policy

Suppressions are permitted but discouraged. When a finding is a true
false-positive:

1. **Narrowest possible scope** — single line, single rule, never blanket.
2. **Comment required** explaining why this is a false positive (or why
   the trade-off is acceptable).
3. **PR review notes** the suppression so future-me sees it.

Examples:
```java
// SpotBugs: false positive — the resource is closed by Spring's lifecycle
@SuppressFBWarnings(value = "OS_OPEN_STREAM",
    justification = "Closed by Spring lifecycle in DefaultBlobStore.close()")

// PMD: enhanced-for is correct here — body has ordering-sensitive side effects
@SuppressWarnings("PMD.AvoidEnhancedForLoop")
```

### CI Pipeline Shape

GitHub Actions, one workflow file (`.github/workflows/ci.yml`) with
parallel jobs so failures surface fast:

```yaml
jobs:
  spotless:    # fastest; fail-fast
  checkstyle:
  pmd:
  spotbugs:
  archunit:    # part of test, but isolated for visibility
  modulith:    # ApplicationModules.verify()
  unit-tests:  # excludes integration tests
  integration-tests:
  coverage:    # depends on unit + integration; enforces thresholds
```

Each job runs `./mvnw <specific-goal>` so the CI status check name
matches the tool. Branch protection (ADR-010) requires all of them
green before merge.

## Rationale

- **Strict severity** matches the owner's mandate and the high-quality
  standards expressed across the ADRs. Any compromise here would
  undermine the entire point of investing in the toolchain.
- **Fail-on-violation > warn** removes the slow degradation pattern
  where warnings accumulate and become invisible.
- **Tool-encoded rules > prose conventions** — a Checkstyle rule will
  enforce ADR-007 every time a PR is opened; a CLAUDE.md note relies
  on someone remembering.
- **Per-module coverage thresholds** acknowledge that engine code (pure
  logic, well-suited to test) deserves a higher bar than infrastructure
  glue.
- **Local = CI** prevents "works on my machine" surprises. The same
  command runs the same checks.
- **Spring Modulith inclusion** isn't redundant with the package
  Checkstyle rule — Modulith catches cross-module *runtime* references
  that static analysis would miss.

## Consequences

**Positive**
- Style debates resolved at config-write time, never re-litigated
- New contributors (or sub-agents) can't violate conventions
  unintentionally — the build refuses
- Quality is uniform across stories; PR reviews focus on judgment
- Architecture decisions in ADRs become enforceable, not aspirational

**Negative**
- **Initial setup cost** — configuring 7 tools with project-specific
  rules takes real effort. Amortized over 70 stories, it's clearly
  worthwhile.
- **Occasional friction** when a tool flags something legitimate that
  doesn't fit a rule cleanly. Mitigated by the suppressions policy.
- **CI duration** grows with every tool. Mitigation: parallel CI jobs;
  target full pipeline < 3 minutes.
- **Tool upgrades** require care — major-version bumps in PMD / SpotBugs
  may add new rules that fail existing code. Pin versions; upgrade as
  intentional stories.

## Alternatives Considered

- **Use only Spotless + Checkstyle (lightweight)** — rejected; misses
  bug detection (SpotBugs) and architectural rules (ArchUnit,
  Modulith).
- **SonarQube as the umbrella tool** — viable, especially for SaaS
  phase. Rejected for v1 because it adds a server-side dependency
  (or a paid SonarCloud account) that solo dev doesn't justify yet.
  Defer; the underlying tools we're using all feed Sonar if we adopt
  it later.
- **Warning-only with manual triage** — rejected per owner's directive.
- **Lower coverage thresholds (e.g. 60% engine)** — rejected; engine
  code is the most important to verify and the easiest to test.

## Notes

- A pre-commit hook (`pre-commit` framework, or simple bash) can run
  `spotless:apply` + `checkstyle:check` for fast local feedback. Will
  be added in EPIC-1 (S-1.1).
- The `/quality` skill should run `./mvnw verify` end-to-end. If
  current implementation runs only the formatters/linters, extend it.
- A future ADR may revisit this when SaaS phase begins (Sonar adoption,
  external static analysis service, etc.).
- ArchUnit rules are themselves tests — they live in
  `src/test/java/.../ArchitectureTest.java` per module that has
  module-specific rules, plus a project-wide `ProjectArchitectureTest.java`.
