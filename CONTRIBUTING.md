# Contributing to retirement-planner

Thanks for your interest. This document captures the workflow, the
universal Definition of Done that every story must satisfy, and the
most-common gotchas.

The repository's planning artifacts live in `.sdlc/artifacts/`. Read
[`CLAUDE.md`](CLAUDE.md) and the relevant ADRs in
[`.sdlc/artifacts/adr/`](.sdlc/artifacts/adr/) before starting work.

## Workflow

This project follows **trunk-based development** per
[ADR-010](.sdlc/artifacts/adr/ADR-010-branching-strategy.md).

```
main (protected, always shippable)
└── feature/issue-NN-short-description    short-lived, PR back to main
    └── tags vX.Y.Z                       releases
```

Per-story workflow:

1. Pick a story from the Project board's "Ready" column
2. Read its linked Epic / PRD / ADR references
3. Move the issue to **In Progress** (manually or via assignment)
4. `git checkout -b feature/issue-NN-short-description` from `main`
5. Implement, with tests
6. Run `./mvnw verify` locally — this **must** pass
7. Run `/quality fix`, `/java-review`, `/retirement-style` before commit
8. Commit (reference issue: `git commit -m "Add X (#NN)"`)
9. Push and `gh pr create --base main` with `Closes #NN` in the body
10. CI runs all 9 status checks — they must pass before merge
11. `gh pr merge --squash --delete-branch` after CI green

Releases are **tags on main**, not branches. See ADR-010 for details.

## Universal Definition of Done

A story cannot be closed until **every** item below is true. This is
not a suggestion list — these are the gates the project enforces.

### Code quality (ADR-009)
- [ ] `./mvnw verify` passes locally
- [ ] All 9 CI status checks green:
  - `quality / spotless`, `quality / checkstyle`, `quality / pmd`,
    `quality / spotbugs`, `quality / archunit`, `quality / coverage`
  - `modulith / verify`
  - `tests / unit`, `tests / integration`
- [ ] Coverage thresholds met:
  - Engine modules (`contribution`, `tax`, `bucket`, `simulation`,
    `allocation`): **85% line / 75% branch**
  - Domain modules (`plan`, `scenario`): 75% line / 65% branch
  - Infrastructure (`returns`, `shared`): 75% line / 65% branch
  - API surface (`api`): 60% line / 50% branch
  - Project total (after exclusions): 70% line / 60% branch
- [ ] No new suppressions OR each new suppression has a justification
      comment per the ADR-009 suppressions policy

### Tests
- [ ] Unit tests added/updated for changed logic
- [ ] Integration tests added/updated where boundary behavior matters
- [ ] Tests are deterministic (no `Math.random` outside seeded contexts;
      no `Instant.now()` in assertions)
- [ ] Test names describe behavior, not implementation

### Architecture (ADR-002, ADR-008)
- [ ] `ProjectArchitectureTest` passes (constructor injection,
      controllers thin, simulation has no event publisher, internals
      private)
- [ ] `ApplicationModulesIntegrationTest` passes (Spring Modulith
      boundary verification)
- [ ] Cross-module access only through public API packages —
      `<module>/internal/` is private
- [ ] Hot-path inter-module calls use direct method calls;
      `@ApplicationModuleListener` events only at workflow boundaries
      (scenario saved, run completed, deletion)

### Money & precision (ADR-007)
- [ ] All monetary values use `Money` value record
- [ ] No `double` / `float` outside `simulation.montecarlo.internal`
      (Checkstyle catches this)
- [ ] `BigDecimal` constructed from String literals, never from a
      double literal (PMD catches this)
- [ ] Rates stored as `BigDecimal` decimals (0.0245), not percentages

### API & contracts
- [ ] New public API documented in Javadoc
- [ ] New endpoints generate clean OpenAPI output
- [ ] Money is serialized as `{"amount":"...","currency":"USD"}`
      (string amount to preserve precision in JS clients)
- [ ] Error responses follow the global `@ControllerAdvice` shape

### Documentation & traceability
- [ ] Story's Acceptance Criteria all checked
- [ ] Relevant ADRs referenced in the PR description
- [ ] PRD FR/NFR numbers cited where applicable
- [ ] If behavior changed, README / docs updated to match

### Hygiene
- [ ] No new TODOs without an issue link
- [ ] No commented-out code
- [ ] UTC timestamps only (`Instant`, not `LocalDateTime.now()`)
- [ ] Constructor injection only (no `@Autowired` on fields)
- [ ] No fully-qualified class names in code bodies (use imports)
- [ ] Stream API instead of enhanced-for loops
- [ ] `Optional<T>` for absent values; `null` only in JSON DTOs
- [ ] Sealed interfaces for value-like polymorphism (Bucket,
      FundingPolicy, SpendingPolicy, LifecyclePolicy)

### Output is not financial advice (NFR-8)
- [ ] No copy or comment frames output as a recommendation
- [ ] Any new endpoint surfacing results includes the `disclaimer`
      field in the response envelope
- [ ] User-facing strings frame outputs as projections, not advice

## Suppressions

Per [ADR-009](.sdlc/artifacts/adr/ADR-009-quality-gates.md), suppressions
are permitted but discouraged. When you suppress a tool finding:

1. **Narrowest scope** — single line, single rule, never blanket
2. **Justified comment** — explain *why* this is a false positive or
   why the trade-off is acceptable
3. **Note in PR review** — the PR description names the suppression so
   reviewers see it
4. **Pre-flag in the issue** — if you anticipate a suppression while
   reading the story, capture *why* in the issue's "Suppression notes"
   block before opening the PR

## Quality skills

Run these before committing:

| Skill | Purpose |
|---|---|
| `/quality fix` | Auto-format and run all linters |
| `/retirement-style` | Project-specific style and architecture review |
| `/java-review` | Generic Java pattern check |
| `/test` | Run the test suite |
| `/code-review` | Final diff review |

## Common gotchas

- **`compose.yaml` is for Podman** — Docker substitute (licensing). The
  syntax is identical; documentation says `podman compose` not `docker
  compose`.
- **Java 25** is required. Spring Boot 4.x baseline.
- **Branch protection requires squash merge** — your local feature
  branch's commit history is not preserved on `main`.
- **CI must run at least once before status checks can be required** —
  if you add a new CI job, the first PR after that change won't
  enforce it; subsequent PRs will.

## License

By contributing to this project, you agree that your contribution is
licensed under [PolyForm Noncommercial 1.0.0](LICENSE) plus the
AI/ML training prohibition in [LICENSE-ADDENDUM.md](LICENSE-ADDENDUM.md)
§3.

## Questions

Open a GitHub Discussion or comment on a relevant issue.
