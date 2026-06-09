---
name: retirement-style
description: Verify code adheres to retirement-planner's project-specific style and architectural conventions before commit. Catches violations the generic /quality and /java-review skills miss because they're project-specific. Use after writing or modifying Java code, before committing.
---

# Retirement Planner Style Check

This skill enforces the project-specific conventions captured in
ADR-002, ADR-003, ADR-007, ADR-008, and CLAUDE.md. Run it as a final
check before committing.

The mechanical checks (Spotless, Checkstyle, PMD, SpotBugs, ArchUnit,
Modulith verification, JaCoCo) are bound to `mvn verify` and must pass.
This skill complements them with the **judgment calls** an automated
tool can't make.

## Step 1 — Run the full quality suite

```bash
./mvnw verify
```

If anything fails, fix it before continuing. The build is configured to
fail on any violation per ADR-009.

## Step 2 — Read the diff with these checks in mind

For every changed Java file, verify:

### Money & Precision (ADR-007)

- [ ] All monetary values use the `Money` value record, never `BigDecimal` directly for money fields
- [ ] No `double` or `float` outside `simulation.montecarlo.internal` (Checkstyle catches this; verify any new package boundaries)
- [ ] BigDecimal constructed from `String` literals or `BigDecimal.valueOf(...)`, never from a `double` literal
- [ ] Rates stored as `BigDecimal` decimals (0.0245), not as percentages (2.45)
- [ ] Currency mixing impossible by construction (operations check currency equality)

### Null Safety & Iteration (CLAUDE.md)

- [ ] Methods that can fail to find a value return `Optional<T>`, not `null`
- [ ] DTOs are the only place `null` may appear (for JSON serialization)
- [ ] No enhanced-for loops (`for (T t : iterable)`); Stream API used instead
- [ ] When PMD's `NoEnhancedForLoop` is suppressed, the comment justifies why ordering-sensitive side effects can't compose as a stream

### Imports (CLAUDE.md)

- [ ] No fully-qualified class names in code bodies (e.g. `org.assertj.core.api.Assertions.assertThat(...)`); add an import
- [ ] Suppression with `@SuppressWarnings("PMD.NoFullyQualifiedNames")` requires a justified comment about a true import collision

### Domain Model (ADR-002)

- [ ] New `Bucket`, `FundingPolicy`, `SpendingPolicy`, or `LifecyclePolicy` types are added to the corresponding sealed interface, not as new top-level interfaces
- [ ] Bucket value types live in the `plan/` module; bucket engine code lives in `bucket/`
- [ ] New `Account` types are enum values on `AccountType`, not subclasses
- [ ] Sleeves correctly carry `SleeveYieldPolicy`; tax treatment is per-account, not per-sleeve

### Tax Engine (ADR-004)

- [ ] Tax-law constants come from YAML config, not hard-coded
- [ ] New state tax additions follow the schema in S-3.2 (model, brackets/flat, retirement_subtractions, source citation)
- [ ] RMD code uses the IRS Uniform Lifetime Table from config, not formulas

### Contributions (ADR-003)

- [ ] §603 routing tested when adding new contribution paths
- [ ] §415(c) cap aggregates employee + employer per plan per year
- [ ] Limits sourced from `irs-limits.yaml`, not constants

### Modulith Boundaries (ADR-008)

- [ ] Cross-module imports go through public API packages only — no `<module>.internal.*` access from other modules
- [ ] Hot-path inter-module calls (per-month / per-year inside simulation) use injected interfaces, NOT `ApplicationEventPublisher` or `@ApplicationModuleListener`
- [ ] Cold-path workflow boundaries (scenario saved, run completed, deletion) use Modulith events
- [ ] When in doubt: 3-question hot-path test from CLAUDE.md (called inside per-month/year loop? caller needs result? synchronous?) → if all yes, direct call

### Architecture (ADR-001 / CLAUDE.md)

- [ ] Constructor injection only — no `@Autowired` on fields or setters
- [ ] Controllers delegate to services; no business logic in `@RestController` classes
- [ ] Timestamps use `Instant.now()` (UTC), not `LocalDateTime.now()` or `new Date()`
- [ ] No cloud-SDK dependencies in production code (kept behind `BlobStore` interface per ADR-001)

### SaaS Readiness (ADR-001 / ADR-002)

- [ ] New aggregate roots carry `tenantId`
- [ ] Repository queries filter by `tenantId` (or use a Hibernate filter that does)

### Output is Not Financial Advice (NFR-8)

- [ ] No copy or comment frames output as a recommendation ("you should retire at 62")
- [ ] User-facing strings frame outputs as projections and trade-offs
- [ ] If a new endpoint surfaces results, the response envelope includes the `disclaimer` field

## Step 3 — Suppression review

If a tool was suppressed in this PR:

- [ ] Suppression is the narrowest possible scope (single line, single rule)
- [ ] A comment on the suppression explains *why* it's a false positive or why the trade-off is acceptable
- [ ] The PR description notes the suppression for visibility

## Step 4 — Final pass

- [ ] Diff is one story or smaller (per ADR-010 TBD discipline)
- [ ] Branch name follows `feature/issue-NN-short-description`
- [ ] Commit message references the issue number
- [ ] PR template filled out completely

## Output

Report findings as a categorized list. For each finding, cite the ADR
or CLAUDE.md section that the violation contradicts and propose the
fix. Do not auto-apply fixes — the user reviews and accepts.

## When NOT to use this skill

- For pure documentation changes (markdown edits in `.sdlc/`)
- For build-config-only changes that don't touch Java
- During EPIC-1 bootstrap when many of the modules don't exist yet —
  the skill flags as advisory, not blocking, until module structure is
  in place
