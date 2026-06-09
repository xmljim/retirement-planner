# retirement-planner

A retirement planning application that produces month-by-month projections
across the **accumulation**, **bridge**, and **drawdown** phases — modeling
goal-based buckets, tax-aware withdrawals, glide-path asset allocation, and
Monte Carlo uncertainty.

> **Status:** planning-complete, pre-implementation.
> Discovery, ADRs, PRD, and epic decomposition are in `.sdlc/artifacts/`.
> Implementation begins with EPIC-1 (Foundation).

## What's In This Repository Right Now

- `.sdlc/artifacts/discovery/` — problem framing and scope (DISC-001)
- `.sdlc/artifacts/adr/` — 10 architectural decisions (ADR-001..010)
- `.sdlc/artifacts/prd/` — product requirements (PRD-001)
- `.sdlc/artifacts/epics/` — 8 epics, ~70 stories
- `CLAUDE.md` — engineering conventions and skill references
- `pom.xml` + `config/` + `.github/workflows/` — Maven build with the full
  quality-gate suite (Spotless, Checkstyle, PMD, SpotBugs, ArchUnit,
  Spring Modulith, JaCoCo) wired to fail the build on any violation
- `src/` — Spring Boot 4.x scaffolding only; substantive engines land in EPIC-1+

## License

This project is published under the **PolyForm Noncommercial 1.0.0** license
(see [`LICENSE`](LICENSE)) **with an additional addendum** that explicitly
prohibits use of this software as training data for AI / machine-learning
systems (see [`LICENSE-ADDENDUM.md`](LICENSE-ADDENDUM.md) §1).

Public visibility on GitHub is **not** consent to AI training, scraping,
or commercial use. Personal use, research, and noncommercial work are
welcome under the terms of the license.

For commercial licensing, AI/ML licensing, or other special-purpose
permissions, contact the copyright holder via the GitHub profile.

A `robots.txt` at the repo root documents opt-out signals for AI crawler
user-agents as a supplementary signal; the legally operative document is
the LICENSE addendum.

## Architecture at a Glance

- **Backend**: Java 25 + Spring Boot 4.x + Spring Modulith
- **Frontend**: separate repo (planned), React + TypeScript
- **Persistence**: PostgreSQL (live state) + Parquet (immutable snapshots)
- **Auth**: passkeys (architecturally) — stubbed for solo-phase v1
- **Local dev**: Podman compose
- **Branching**: trunk-based (ADR-010)
- **Quality gates**: strict — `mvn verify` runs the full suite (ADR-009)

See [`.sdlc/artifacts/adr/README.md`](.sdlc/artifacts/adr/README.md) for the
full architectural-decision index.

## Building

```bash
./mvnw verify        # full build with all quality gates
./mvnw test          # unit tests only
./mvnw spotless:apply  # auto-format
```

A passing `mvn verify` is the precondition for every PR.

## Running

```bash
podman compose up -d   # bring up Postgres on localhost:5433
./mvnw spring-boot:run # uses the local profile by default
```

The app listens on **http://localhost:8181**. Health endpoint:
`http://localhost:8181/actuator/health`.

### Postgres on localhost:5433

The compose file maps Postgres to host port **5433** (not the default
5432) so it doesn't collide with any other Postgres container you may
already be running. The container itself listens on 5432 internally;
only the host mapping changed. If you need to connect with `psql`:

```bash
psql -h localhost -p 5433 -U retirement retirement_planner
```

### Podman, not Docker

Per [ADR-001](.sdlc/artifacts/adr/ADR-001-platform-and-infrastructure.md),
this project's container runtime is **Podman**. The compose file uses
the standard Compose syntax, which is also Docker-compatible — so
contributors who only have Docker installed can substitute
`docker compose` for `podman compose`. All project documentation and
scripts say `podman`.

## Contributing

This is currently a personal project. Issues and PRs are welcome under the
project's license terms. By submitting a contribution, you agree that your
contribution is licensed under PolyForm Noncommercial 1.0.0 plus the AI
restriction in the addendum (see [`LICENSE-ADDENDUM.md`](LICENSE-ADDENDUM.md)
§3).

## Disclaimer

Output produced by this software is **illustrative, not financial advice**.
The model captures the most material provisions of US federal and state tax
law for retirement planning, but is not a substitute for a licensed
financial advisor or tax professional.
