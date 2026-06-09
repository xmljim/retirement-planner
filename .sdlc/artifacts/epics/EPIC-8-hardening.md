# EPIC-8: Hardening & Observability

**Milestone**: M8 — Hardening
**Total points**: ~20
**Goal**: v1 release-ready: structured logs, metrics, additional state tax, broader tests, disclaimer UX, accessibility pass.

## Stories

### S-8.1 — Structured logging with JSON layout
**As an** operator **I want** logs in JSON with consistent fields **so that** they're searchable in any log aggregator.

**Acceptance criteria**
- SLF4J + logstash-logback-encoder configured for `prod` profile; pretty-print for `local`
- Standard fields per log: timestamp (ISO 8601 UTC), level, logger, message, thread, traceId (when present), tenantId
- Run-boundary logs at INFO: run start (with seed, sim count), run end (with duration, sim count, success rate)
- Tests: log capture verifies expected fields present

**Points**: 2
**Traces to**: NFR-11

---

### S-8.2 — Metrics: run duration histogram
**As an** operator **I want** Micrometer histograms for run duration **so that** performance regressions are caught.

**Acceptance criteria**
- Micrometer registered; expose `/actuator/prometheus`
- Histograms: `simulation.run.duration` (tags: mode=deterministic|montecarlo, sims), `tax.computeYearly.duration`, `bucket.evaluate.duration`
- Counters: scenario saves, runs started, runs cached
- Tests: a small run produces non-zero histogram samples

**Points**: 3
**Traces to**: NFR-11

---

### S-8.3 — Add 2 additional states to tax engine
**As a** SaaS-future user **I want** more states supported **so that** my plan is realistic.

**Acceptance criteria**
- 2 additional state YAML configs added (states determined during M3 planning per PRD §9), conforming to the retirement-aware schema from S-3.2
- Each YAML carries a source citation (state DOR URL, retrieval date) per ADR-004 audit policy
- Each new state has unit tests against canonical retiree-income examples (not filing-grade): a typical mix of SS + IRA withdrawal + pension, asserting state tax is within a documented tolerance of the published worked example
- Unsupported states still produce a clear error

**Points**: 3
**Traces to**: FR-6.4, ADR-004

---

### S-8.4 — Disclaimer UX integration with API
**As a** maintainer **I want** every projection/run response to carry the "not financial advice" disclaimer string **so that** frontend rendering is consistent.

**Acceptance criteria**
- Response envelope includes `disclaimer: "Output is illustrative and not financial advice..."` for every projection and run endpoint
- String configurable via `app.disclaimer` (default supplied)
- Tests: every relevant endpoint includes the field

**Points**: 1
**Traces to**: NFR-8

---

### S-8.5 — Passkey integration test
**As a** developer **I want** the passkey path exercised by an integration test **so that** SaaS migration doesn't surprise us late.

**Acceptance criteria**
- Integration test starts the app with `app.auth.mode=passkey`
- Verifies a registered credential authenticates and an unknown credential fails
- Test uses a small WebAuthn test harness (likely Spring Security's test support)
- This test is not part of the default Maven `test` profile; runs in CI under a `auth-integration` profile

**Points**: 5
**Traces to**: FR-10.1, FR-10.2, FR-10.3, ADR-001

---

### S-8.6 — Architecture documentation generation
**As a** maintainer **I want** Modulith's PlantUML/AsciiDoc module docs generated and checked in **so that** architecture stays self-documenting.

**Acceptance criteria**
- Modulith `Documenter` invoked at build time; output to `docs/architecture/modules/`
- Pre-commit or CI check verifies the generated docs match what's checked in (regenerate locally to fix)
- README links to the generated docs

**Points**: 2
**Traces to**: ADR-008

---

### S-8.7 — Accessibility & content review pass (API-level)
**As a** maintainer **I want** the API surface reviewed for accessibility-relevant concerns (clear error messages, no jargon in user-facing strings) **so that** the eventual frontend has good copy to work with.

**Acceptance criteria**
- All user-facing strings (error messages, disclaimer, warnings emitted in run reports) reviewed for clarity
- Errors include actionable next-step language ("Add a Roth 401(k) account or remove the catch-up portion")
- No financial-advice phrasing slipped in (per NFR-8)
- Review notes captured in `docs/development/content-review.md`

**Points**: 2
**Traces to**: NFR-8

---

### S-8.8 — Release readiness checklist
**As a** maintainer **I want** a release checklist completed before v1 tag **so that** nothing important is forgotten.

**Acceptance criteria**
- Checklist file: tests green, coverage thresholds met, security review run (`/security-review`), README + DEVELOPER_GUIDE up to date, OpenAPI spec versioned, demo data prepared, disclaimer reviewed by owner
- Each item checked off in a release PR
- `/release` skill drives the actual cut

**Points**: 2
**Traces to**: process
