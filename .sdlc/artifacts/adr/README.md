# Architectural Decision Records

Each ADR captures one significant architectural decision: the context, the
decision itself, why we chose it, what we gave up, and what alternatives we
rejected. ADRs are versioned with the codebase; supersession is explicit
(create a new ADR; mark the old one Superseded).

## Index

| ID  | Title | Status |
|-----|-------|--------|
| [ADR-001](ADR-001-platform-and-infrastructure.md) | Platform & Infrastructure (repo topology, auth, Podman, deployment) | Accepted |
| [ADR-002](ADR-002-domain-model.md) | Domain Model (household, spouses, accounts, buckets, rollovers) | Accepted |
| [ADR-003](ADR-003-accumulation-and-contribution-model.md) | Accumulation Phase & Contribution Model | Accepted |
| [ADR-004](ADR-004-tax-engine.md) | Tax Engine (federal, state, RMDs, Roth conversions) | Accepted |
| [ADR-005](ADR-005-monte-carlo-and-returns.md) | Monte Carlo Simulation & Historical Returns | Accepted |
| [ADR-006](ADR-006-scenario-persistence-and-versioning.md) | Scenario Persistence & Versioning (Postgres + Parquet) | Accepted |
| [ADR-007](ADR-007-money-representation.md) | Money Representation & Precision (BigDecimal, scale 6/2) | Accepted |
| [ADR-008](ADR-008-module-boundaries-and-modulith.md) | Module Boundaries with Spring Modulith | Accepted |
| [ADR-009](ADR-009-quality-gates.md) | Quality Gates — Toolchain & Enforcement | Accepted |
| [ADR-010](ADR-010-branching-strategy.md) | Branching Strategy — Trunk-Based Development | Accepted |
