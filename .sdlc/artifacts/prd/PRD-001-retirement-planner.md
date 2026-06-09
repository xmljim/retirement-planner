# PRD-001 — Retirement Planner

- **Status**: Draft
- **Date**: 2026-06-08
- **Owner**: Jim Earley
- **Inputs**: [DISC-001](../discovery/DISC-001-20260608.yaml), [ADR-001](../adr/ADR-001-platform-and-infrastructure.md)–[ADR-007](../adr/ADR-007-money-representation.md)

---

## 1. Overview

A web application that lets a user define a retirement plan as a structured
set of inputs — demographics, accounts (with sleeves), salary and
contributions, goal-based buckets, tax assumptions, asset allocation
glide path — and produces a continuous month-by-month projection across
the **accumulation**, **bridge**, and **drawdown** phases. Monte Carlo
simulation reports probability of success and shows which goals get
sacrificed under stressed market conditions. Multiple scenarios can be
saved, cloned, and visually compared.

Solo single-user at v1; passkey-based authentication and multi-tenancy
are architected from day one for an eventual SaaS phase.

## 2. Goals & Non-Goals

### Goals
- Faithfully model the **accumulation** phase the spreadsheet glosses
  over: per-account contributions with employer match, IRS limits with
  catch-up tiers, salary growth and override events.
- Model the **bridge** phase (retirement → SS/Medicare) with explicit
  healthcare costs and a configurable Bridge bucket.
- Model the **drawdown** phase with full tax mechanics: federal +
  state brackets, taxable Social Security, RMDs at 73/75, Roth
  conversion ladders.
- First-class **buckets** for goal-based spending (Bridge, Travel,
  Bucket-list, Healthcare, Legacy) with extensible types.
- **Adaptive spending**: `SpendingPolicy` per bucket lets users
  encode realistic responses to market conditions and aging.
- **Account sleeves**: model cash sleeves in tax-advantaged accounts
  correctly — real yield, tax treatment of the wrapper.
- **Monte Carlo** with glide-path asset allocation and historical
  block bootstrap; report success probability and per-bucket sacrifice
  statistics.
- **Scenario management**: save, name, clone, and visually compare
  scenarios, with immutable Parquet snapshots for history.

### Non-Goals (v1)
- Mobile-native apps (responsive web only)
- Account aggregation via Plaid or similar (manual balance entry)
- Multi-user collaboration on a single plan
- Tax-loss harvesting / asset-location optimization
- Real-time market data integration
- Financial-advice features (planning tool only — disclaimer required)
- Insurance modeling (LTC, life, annuities); annuities deferred
- Estate-planning depth beyond a Legacy target
- NIIT and AMT modeling
- Mega Backdoor Roth (after-tax 401(k) → Roth conversion paths)
- Joint and Last Survivor RMD table (Uniform Lifetime only in v1)
- Quarterly estimated tax payment cash-flow modeling (annual lump in December)

## 3. Personas

### P1: The Owner (v1 primary user)
- Late-50s pre-retiree with spouse, planning early retirement
- Comfortable with finance concepts (401(k), Roth, RMDs, FRA)
- Wants knobs to tune; will not accept a black-box recommendation
- Stack-savvy: will run the app locally with Podman initially

### P2: The DIY Planner (SaaS-phase user)
- Personal-finance hobbyist; comfortable entering balances and
  contribution rates
- Wants probabilistic answers ("what's the chance I run out?") more
  than point estimates
- Cares about portability — wants to download/export their plan

### P3: Future Reviewer (out of scope, but architecture-aware)
- A financial advisor a user might share read-only access with later.
  Shapes the multi-tenant, scenario-share design even though no UI
  for them in v1.

## 4. Functional Requirements

Numbered for traceability. Each requirement maps to one or more user
stories in the epic decomposition.

### 4.1 Plan & Household Setup
- **FR-1.1** User creates a `Plan` with metadata (name, description, target retirement date)
- **FR-1.2** User defines a `Household`: filing status (Single, MFJ, MFS, HoH, QW), state of residence
- **FR-1.3** User defines 1 or 2 `Person` records per Plan: name, DOB
- **FR-1.4** Per Person: salary profile (current salary, annual growth rate, list of `SalaryOverride` events with effective date + new salary, optional `BonusPolicy`)
- **FR-1.5** Per Person: Social Security configuration (FRA monthly amount, elected SS age 62–70, derived elected SS date)
- **FR-1.6** Spousal SS handled correctly: each spouse has their own FRA amount; spousal benefit calculated automatically when greater than own benefit

### 4.2 Accounts & Sleeves
- **FR-2.1** User defines `Account` records: type (Trad 401(k), Roth 401(k), Trad IRA, Roth IRA, HSA, Taxable Brokerage, Cash, Pension), owner (Person or Joint)
- **FR-2.2** Each account supports one or more `AccountSleeve` records: kind (Cash, AssetAllocation, FixedAllocation), balance, yield policy
- **FR-2.3** Default for any new account: a single `AssetAllocation` sleeve holding the full balance
- **FR-2.4** Per contributing account: optional `ContributionPolicy` with employee contribution (% of salary or fixed $), optional escalation, optional employer match (tiered)
- **FR-2.5** Engine enforces IRS limit hierarchy: §402(g) for elective deferrals, §408 for IRA, §223 for HSA, §415(c) for total DC plan (employee + employer combined)
- **FR-2.6** Catch-up tiers (50+, 60+ super catch-up per SECURE 2.0) applied automatically based on age in the contribution year
- **FR-2.7** IRS limits sourced from a config file; published years use real values, future years are projected by inflation
- **FR-2.8** SECURE 2.0 §603: when a Person's **prior-year FICA wages** from a given employer exceed the indexed threshold (config-driven), catch-up contributions to that employer's plan are automatically routed to a Roth designated sub-account. If no Roth designated account exists for that employer, the catch-up portion is disallowed and a warning is emitted in the run report.
- **FR-2.9** SECURE 2.0 §604 (optional): per contributing account, a `matchAsRoth` flag indicates the employee has elected Roth treatment of employer match. When set, matched dollars accrue to a Roth account and the employee's taxable wages for that year include the match amount (handled by the tax engine).
- **FR-2.10** First-year input: per Person per Employer, user enters `priorYearFicaWages` (most recent W-2) so the §603 rule is applicable in year 1 of the simulation. Subsequent years derive from the salary profile.

### 4.3 Buckets
- **FR-3.1** User defines `Bucket` records of type: BridgeBucket, RecurringAnnualBucket (e.g. travel), OneTimeGoalBucket (e.g. RV), HealthcareBucket, LegacyTargetBucket
- **FR-3.2** Per bucket: `FundingPolicy` (PrefundAtRetirement | SinkingFund | PullOnDemand | Constraint)
- **FR-3.3** Per bucket: `SpendingPolicy` (Fixed | AcrossTheBoardReduction | PriorityFirst | DeferredSink | GuytonKlingerGuardrail | InflationFloor | AgeBasedDecline | Composite[…])
- **FR-3.4** Per bucket: priority (integer; lower = higher priority), used by PriorityFirst
- **FR-3.5** Per bucket: hosting (sleeve-hosted, account-hosted, or virtual) — derived from funding policy + user-selected host
- **FR-3.6** Per bucket: `LifecyclePolicy` defining what happens to leftover balance at end-of-life (SweepTo bucket | SweepToAccount | SweepProportional | Retain)
- **FR-3.7** User can add `BucketReallocationEvent` for ad-hoc mid-plan moves with amount mode (FullBalance | Fixed | Percent | ExcessOver(reserve))
- **FR-3.8** New bucket types can be added by implementing the `Bucket` interface and registering — no engine changes (verified by an extension test)

### 4.4 Rollovers & Conversions
- **FR-4.1** User can define `RolloverEvent`: effective date, source account, destination account, amount mode (FullBalance | Fixed | Percent)
- **FR-4.2** User can define `RothConversion`: tax year, source Trad account, destination Roth account, amount mode (Fixed | FillBracket(targetMarginalRate))
- **FR-4.3** Engine processes all events in date order alongside reallocations; tax engine handles conversion as ordinary income

### 4.5 Assumptions & Asset Allocation
- **FR-5.1** Per Plan: `Assumptions` for inflation rate, contribution-limit growth rate, cash interest rate (default sleeve yield)
- **FR-5.2** Per Plan: `AssetAllocationPolicy` as a glide path — list of `(referenceAge, weights)` points; weights interpolate linearly between points
- **FR-5.3** Asset classes (v1 fixed set): US Large Cap, US Small Cap, International Developed, Emerging Markets, US Aggregate Bonds, US TIPS, Cash
- **FR-5.4** Historical returns dataset bundled with the app; one Parquet file per asset class (monthly returns, multi-decade history)

### 4.6 Tax Engine
- **FR-6.1** Per simulation year, engine computes `TaxResult` (gross income, AGI, taxable income, federal tax, state tax, total tax, effective rate, marginal rate)
- **FR-6.2** Federal brackets, standard deduction, and SS taxability thresholds sourced from year-keyed YAML config
- **FR-6.3** Taxable Social Security computed via IRS provisional-income formula
- **FR-6.4** State tax computed for the user's state of residence; v1 supports federal + at minimum the user's home state + 2 additional commonly-requested states (states determined during epic planning, e.g. NC, FL, NY based on user input)
- **FR-6.5** RMDs computed annually using IRS Uniform Lifetime Table (config-driven); engine forces top-up withdrawal if user-planned withdrawals don't meet RMD; emits `RmdShortfall` warning if account would drain below zero
- **FR-6.6** RMD applies to Trad IRA, Trad 401(k); not Roth IRA or post-2024 Roth 401(k)
- **FR-6.7** Tax computed annually; year-end debit applied to the configured tax-payment source account

### 4.7 Simulation Engine
- **FR-7.1** Engine produces a continuous monthly projection from "today" to end-of-plan (configurable, default age 95 for primary)
- **FR-7.2** Each month's row classifies by phase: ACCUMULATION, BRIDGE, DRAWDOWN
- **FR-7.3** Engine supports two run modes: deterministic (single trajectory) and Monte Carlo (N simulations)
- **FR-7.4** Monte Carlo uses block bootstrap (default 12-month blocks, configurable) sampling from the historical returns dataset
- **FR-7.5** Glide-path allocation evaluated each month
- **FR-7.6** Each year boundary: tax computation, RMD enforcement, bucket `SpendingPolicy.evaluate()` per bucket, lifecycle/reallocation event processing
- **FR-7.7** Performance target: 1000 simulations × 50-year plan completes in < 5 seconds server-side
- **FR-7.8** Run is deterministic given (Plan snapshot + seed); same inputs + same seed = identical output

### 4.8 Scenario Management
- **FR-8.1** User can save a scenario with a name and description
- **FR-8.2** User can clone an existing scenario (full copy of inputs); cloned scenario is independent
- **FR-8.3** User can run a saved scenario at any time; runs are cached by `inputsHash` + seed
- **FR-8.4** User can compare 2+ scenarios on overlay charts: portfolio value, success probability, per-bucket spend trajectory
- **FR-8.5** User can delete a scenario; deletion removes the DB row and the BlobStore prefix
- **FR-8.6** Each scenario carries an immutable history of Parquet snapshots (one per save), navigable in the UI
- **FR-8.7** Scenario JSON import/export deferred to v1.1

### 4.9 Reporting & Visualization
- **FR-9.1** Single-scenario view shows: portfolio value over time (deterministic line + Monte Carlo percentile bands 10/25/50/75/90), withdrawals by source, cash flow stack
- **FR-9.2** Single-scenario view shows annual tax detail: federal tax, state tax, RMD, taxable SS, marginal rate
- **FR-9.3** Single-scenario view shows per-bucket detail: target spend, actual spend, scaling decisions, deferral history (for adaptive policies)
- **FR-9.4** Monte Carlo report shows: success probability, year-of-failure histogram, **bucket-sacrifice statistics** ("Travel scaled ≥20% in X% of sims; Legacy missed in Y%")
- **FR-9.5** Display toggle: nominal dollars (default) ↔ real dollars (today's purchasing power)
- **FR-9.6** All charts include a clear "Not financial advice" disclaimer

### 4.10 Authentication
- **FR-10.1** v1 (solo): single hardcoded "owner" principal injected by dev-mode `SecurityFilterChain`; no real auth flow exposed
- **FR-10.2** Architecturally: real `AuthenticationProvider` interface; passkey implementation present and exercised in integration tests but not the default
- **FR-10.3** SaaS phase: passkey provider becomes default via `app.auth.mode=passkey`; no password fallback

## 5. Non-Functional Requirements

- **NFR-1 (Performance)**: 1000-sim Monte Carlo of 50-year plan in < 5 seconds server-side
- **NFR-2 (Precision)**: All money math uses `Money` value type (BigDecimal scale 6 internal, scale 2 display, HALF_EVEN rounding); double permitted only inside the bounded MC inner loop per ADR-007
- **NFR-3 (Reproducibility)**: Run output deterministic given (Plan snapshot + seed)
- **NFR-4 (Tax-law evolvability)**: Federal brackets, state tax tables, IRS contribution limits, IRS RMD table all data-driven (YAML/config)
- **NFR-5 (SaaS-readiness)**: Multi-tenant boundary observed via `tenantId` on every aggregate; no cloud-SDK dependencies in production code paths
- **NFR-6 (Local-dev)**: One-command spin-up via `podman compose up`; no requirement for cloud accounts
- **NFR-7 (Auditability)**: Every simulation row records its phase, contribution lines, withdrawal lines, tax events, and bucket decisions; downloadable as CSV
- **NFR-8 (Privacy / Disclosure)**: Application banner + per-page disclaimer that output is illustrative, not financial advice
- **NFR-9 (Code Quality)**: Java 21+; constructor injection; `Optional` for absent values; Stream API over for-each; Controllers delegate, no business logic; UTC timestamps; per ADR-007 no `double` outside MC inner loop
- **NFR-10 (Test Coverage)**: Engine code (simulation, tax, contribution, MC) ≥ 80% line coverage; reference fixtures replicate Sheet2 outputs within 1% on matched inputs (DISC-001 success criterion)
- **NFR-11 (Observability)**: Structured logs at run boundaries; SLF4J + JSON layout; histogram metrics for run duration
- **NFR-12 (Module Boundaries)**: Spring Modulith enforces module topology per ADR-008; `ApplicationModules.verify()` runs in CI and fails the build on boundary violations. Hot-path inter-module calls use injected interfaces; Modulith events reserved for user-initiated workflow boundaries (scenario saved, run completed)

## 6. UX Outline (high level)

The UI design is owned by `retirement-planner-ui` (separate repo). PRD
references the UX shape only enough to constrain the API:

- **Plan dashboard** — list of saved scenarios, "create new", "compare"
- **Scenario editor** — multi-tab form (Household, People, Accounts, Buckets, Events, Assumptions, Allocation); validation per ADR rules
- **Run view** — chart-heavy, with a knob panel for live re-runs of common parameters (retirement age, SS election age, return assumptions)
- **Compare view** — overlay 2+ scenarios on the same axes
- **Bucket-detail drawer** — per bucket: configured policy, projected spend, sacrifice history under MC

The API exposes a clean OpenAPI spec used by the frontend; spec
generation is part of the backend build.

## 7. Data & Models (summary)

Defined fully in ADR-002 / ADR-006. Top-level aggregates:

- `Plan` (root) → `Household`, 1..2 `Person`, 0..n `Account`, 0..n `Bucket`, 0..n events (Rollover, Conversion, Reallocation), `Assumptions`, `AssetAllocationPolicy`
- `Account` → 1..n `AccountSleeve`, optional `ContributionPolicy`
- `Scenario` → reference to `Plan`, `RunConfiguration` (seed, sim count, MC params)
- `Snapshot` (Parquet) → immutable serialization of Plan + Scenario at a point in time
- `Run` (Parquet) → simulation outputs

## 8. Milestones

Sized for one solo developer. Each milestone ends at a runnable, useful state.

### M1 — Foundation (≈3 weeks)
Spring Boot scaffolding, Postgres + Flyway, package structure (per
ADR-001), Money value type (ADR-007), domain entities (ADR-002 minus
buckets and sleeves), basic CRUD APIs for Plan/Person/Account, stubbed
auth.
**Done when**: A user can create a Plan with two People and a few
Accounts via REST and re-fetch them.

### M2 — Accumulation Engine (≈3 weeks)
Sleeves, ContributionPolicy with employer match, IRS limits config, salary
growth + overrides, deterministic monthly accumulation projection. No
buckets, no taxes, no MC yet.
**Done when**: For a given Plan, the API returns a month-by-month
balance projection from today to retirement that matches a hand-checked
spreadsheet within 1%.

### M3 — Drawdown & Tax Engine (≈4 weeks)
Tax engine (federal + 1 state), RMDs, RothConversions, RolloverEvents,
SS taxability, end-of-plan extension. Still deterministic, still no
buckets.
**Done when**: API returns full lifecycle projection through end of
plan with tax line per year; Sheet2-equivalent inputs reproduce
Sheet2 numbers within 1%.

### M4 — Buckets (≈4 weeks)
Bucket entity with funding/spending/lifecycle policies (Fixed,
PrefundAtRetirement, SinkingFund, PullOnDemand, Constraint;
AcrossTheBoardReduction, PriorityFirst, AgeBasedDecline,
InflationFloor, Composite). Reallocation events. Bridge, Travel,
OneTimeGoal, Healthcare, Legacy concrete types.
**Done when**: API supports defining buckets, projection includes
per-bucket cash flows, end-of-life lifecycle sweep works.

### M5 — Monte Carlo (≈3 weeks)
Historical returns Parquet datasets bundled, BlobStore filesystem
implementation, glide-path allocation, block bootstrap MC, parallel
execution, SpendingPolicy.evaluate() at year boundaries with sacrifice
statistics in output.
**Done when**: 1000 sims × 50 years runs in < 5s; output Parquet
contains percentile bands and per-bucket sacrifice stats.

### M6 — Scenario Management (≈2 weeks)
Snapshots on save, run caching by inputsHash, clone, compare API.
**Done when**: User can save, clone, run, and compare scenarios via
API; snapshots persist to BlobStore.

### M7 — Frontend Integration (≈4 weeks, parallel)
Frontend repo `retirement-planner-ui`: scenario editor, run view,
compare view. Generated from OpenAPI spec.
**Done when**: A user can drive the full v1 feature set through the
browser.

### M8 — Hardening (≈2 weeks)
Observability, structured logs, run-duration metrics, additional state
tax tables, Joint and Last Survivor RMD swap (kept deferred until
needed), broader integration tests, disclaimer UX, accessibility pass.

**v1 release** at end of M8.

## 9. Open Items & Deferred Decisions

- Which two additional states to ship for tax in v1 (decide during M3 planning)
- Bonus modeling shape (lump-sum vs. % of salary; resolved in M2)
- Whether `Annuity` and `Pension` warrant a separate income-stream
  abstraction from `Account` (revisit at M4)
- UI design system / component library (owned by frontend repo)
- Hosting target for SaaS phase (deferred per ADR-001)

## 10. Success Criteria (carried from DISC-001)

- ≥ 1% match to Sheet2 for matched inputs (deterministic mode)
- Multi-account types supported: Trad 401(k), Roth 401(k), Trad IRA, Roth IRA, HSA, Taxable
- Tax model: federal + state + RMDs + Roth conversions + taxable SS
- 1000-sim MC of 50-year plan in < 5s
- New bucket type added without engine change (extension test)
- ≥ 2 scenarios overlaid on the same chart for portfolio value & success probability

## 11. Risk Register

| Risk | Likelihood | Impact | Mitigation |
| --- | --- | --- | --- |
| MC performance budget missed | Medium | High | JMH benchmark in M2/M3; dual-precision (ADR-007); parallel streams |
| Tax law changes mid-development | Low | Medium | Config-driven brackets/limits/RMD table |
| Sealed-interface JPA mapping pain | Medium | Medium | Spike in M1; fall back to discriminator + JSON column if needed |
| Scope creep on buckets | High | Medium | v1 freezes the SpendingPolicy and FundingPolicy lists; new types post-v1 |
| Underestimated bucket reallocation tax interaction | Medium | High | Dedicated unit-test matrix for cross-account reallocation in M4 |
| OpenAPI ↔ frontend drift | Medium | Medium | Spec-first contract; generate frontend types in CI |
| SECURE 2.0 §603 catch-up routing edge cases | Medium | Medium | Decision matrix unit tests in M2: (high-earner × has-Roth-401k × age-tier × multi-employer); explicit warning path when plan lacks Roth designation |
