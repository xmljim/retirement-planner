# EPIC-2: Accumulation & Contribution Engine

**Milestone**: M2 — Accumulation Engine
**Total points**: ~45
**Goal**: Given a Plan, the API returns a month-by-month balance projection from today to retirement that matches a hand-checked spreadsheet within 1%, including IRS limits, employer match, and SECURE 2.0 §603/§604 routing.

## Stories

### S-2.1 — Salary profile with growth and overrides
**As a** user **I want** to define salary as a current value, growth rate, and optional override events **so that** known job changes can be modeled.

**Acceptance criteria**
- `SalaryProfile` value record per Person: `currentSalary`, `annualGrowthRate`, `List<SalaryOverride>`, `Optional<BonusPolicy>`
- Engine method `salaryAt(LocalDate)` returns the active salary for any month, applying overrides at their effective dates and growth between them
- Bonus payout occurs in the configured month each year
- Tests cover: smooth growth, single override, multiple overrides, growth resuming after override

**Points**: 3
**Traces to**: FR-1.4, ADR-003

---

### S-2.2 — IRS limits configuration loader
**As an** engine **I want** IRS contribution limits and SECURE 2.0 thresholds loaded from year-keyed YAML **so that** annual updates don't require code changes.

**Acceptance criteria**
- Bundled config at `resources/data/irs-limits.yaml` with at minimum 2024–2026 published values
- Loader returns limits for any year; future years are projected by `assumptions.contributionLimitGrowthRate`
- Includes: §402(g) base, 50+ catch-up, 60+ super-catch-up; §408 IRA base + 50+; §223 HSA self/family + 55+; §415(c) total DC; §603 high-earner threshold (FICA wages, indexed)
- Loader logs which years are projected vs published
- Tests cover: known year lookup, projected year, limit boundary cases

**Points**: 3
**Traces to**: FR-2.5, FR-2.7, ADR-003

---

### S-2.3 — ContributionPolicy value object
**As a** user **I want** to attach a contribution policy to an account **so that** monthly contributions are derived parametrically.

**Acceptance criteria**
- `ContributionPolicy` record: `employee` (PERCENT_OF_SALARY | FIXED_DOLLAR), `Optional<EscalationPolicy>`, `Optional<EmployerMatch>`, `Optional<startDate/endDate>`
- `EmployerMatch` supports tiered formula: `List<MatchTier(employeePctUpTo, matchPct)>`
- Persisted on the Account entity; sealed-interface fields stored via discriminator + JSON column
- Validation: employer match only valid on 401k/403b accounts
- Tests cover policy round-trip and basic match math (single tier, multi-tier, post-cap)

**Points**: 3
**Traces to**: FR-2.4, ADR-003

---

### S-2.4 — Monthly contribution computation (simple case)
**As an** engine **I want** to compute the per-month employee + employer contribution for one account **so that** balances grow correctly during accumulation.

**Acceptance criteria**
- `ContributionEngine.contributeForMonth(account, person, year, month)` returns a list of `CashFlow` lines (separate for employee, employer match, after-tax)
- Applies §402(g)/§408/§223 to the employee portion based on account type and age
- Applies employer match against post-cap employee
- Applies §415(c) cap to employee + employer combined
- Year-boundary state: per person, per plan, "DC contribs this year" tracker resets at year start
- Tests cover the "Limit hierarchy" examples from ADR-003 (cap-binding employee, cap-binding 415c, escalation across year boundary)

**Points**: 5
**Traces to**: FR-2.5, FR-2.6, ADR-003

---

### S-2.5 — SECURE 2.0 §603 high-earner Roth catch-up routing
**As an** engine **I want** to route catch-up contributions to a Roth designated account when prior-year FICA wages exceed the §603 threshold **so that** SECURE 2.0 compliance is correctly modeled.

**Acceptance criteria**
- Person has `priorYearFicaWages` per Employer (user-entered for year 1; derived from salary thereafter)
- At year start, engine determines whether §603 applies for each Person×Employer combination
- When applicable: catch-up portion of the elective deferral routes to the Roth designated sub-account at the same employer plan (e.g. Roth 401k); base deferral retains its user-chosen treatment
- If no Roth designated account exists for that employer, catch-up portion is **disallowed** and a warning recorded in the run report
- Per-month contribution may produce **two** cash flow lines (base + catch-up split)
- Tests cover: high-earner with Roth 401k, high-earner without Roth 401k, low-earner unaffected, Person crossing age 50 mid-year

**Points**: 5
**Traces to**: FR-2.8, FR-2.10, ADR-003

---

### S-2.6 — SECURE 2.0 §604 optional Roth match
**As a** user **I want** to opt-in to Roth treatment of employer match per account **so that** §604-electing plans are modeled correctly.

**Acceptance criteria**
- `Account.matchAsRoth: Boolean` flag (default false)
- When true: matched contributions accrue to the Roth designated sub-account and a W-2 wages adjustment is recorded for the year (consumed by tax engine in EPIC-3)
- Tests cover: matchAsRoth=false (default tax-deferred), matchAsRoth=true producing a Roth cash flow + W-2 adjustment line, mixed across multiple accounts

**Points**: 3
**Traces to**: FR-2.9, ADR-003

---

### S-2.7 — Account sleeve yield application
**As an** engine **I want** to apply per-sleeve yield each month **so that** cash sleeves earn the configured rate while equity sleeves earn the active glide-path return.

**Acceptance criteria**
- `SleeveYieldPolicy` sealed type with implementations: `FixedRate(BigDecimal annual)`, `MoneyMarket(BigDecimal currentRate)`, `TracksAllocation` (defers to glide path; placeholder until EPIC-5 wires returns)
- `accruePerMonth(sleeve, monthIndex)` returns yield for the month
- For deterministic mode used in this epic, `TracksAllocation` reads a single configured pre-retirement return rate from `Assumptions`
- Tests cover: cash sleeve yield over 12 months, allocation sleeve under deterministic returns, mixed-sleeve account aggregation

**Points**: 3
**Traces to**: FR-2.2, FR-5.1, ADR-002

---

### S-2.8 — Deterministic accumulation projection orchestrator
**As a** user **I want** the API to return a month-by-month projection of all accounts during the accumulation phase **so that** I can see balance growth from today to retirement.

**Acceptance criteria**
- `simulation/AccumulationProjector` orchestrates monthly contribution + yield application across all accounts in a Plan
- Returns a structured `MonthlyProjection` per month with: phase=ACCUMULATION, per-account balances and contribution lines
- REST endpoint `GET /plans/{id}/projection?mode=deterministic` returns the projection from today to retirement
- Tests: a fixture mirroring Sheet2's pre-retirement compounding (same DOB, balance, return rate) reproduces Sheet2 values within 1% (Sheet2's accumulation is naive — single-rate compound — so the test inputs match that)
- Tests: a multi-account fixture with employer match produces the expected retirement-date balances

**Points**: 5
**Traces to**: FR-7.1, FR-7.2, NFR-10, DISC-001 success criterion

---

### S-2.9 — Cash flow ledger persistence
**As a** developer **I want** every cash flow recorded with its source, type, and tax treatment **so that** projections are auditable and downstream tax/RMD computations have a ledger to read.

**Acceptance criteria**
- `CashFlow` record: `LocalDate date`, `AccountRef account`, `CashFlowType` (EMPLOYEE_CONTRIB, EMPLOYER_MATCH, AFTER_TAX_CONTRIB, ROTH_CONVERSION_TAXABLE, WITHDRAWAL_ORDINARY, WITHDRAWAL_QUALIFIED, BUCKET_DRAW, ...), `Money amount`, optional metadata
- Per run, all cash flows captured (in-memory; persisted only when scenario is saved per EPIC-6)
- CSV export endpoint (per NFR-7 auditability) — basic implementation here; UI hooks come later

**Points**: 3
**Traces to**: NFR-7, ADR-003

---

### S-2.10 — Sheet2 fidelity fixture
**As a** maintainer **I want** an integration test that loads a fixture matching the spreadsheet's accumulation inputs and asserts ≤1% deviation **so that** the engine's correctness is mechanically guaranteed against the source-of-truth model.

**Acceptance criteria**
- Fixture in `src/test/resources/fixtures/sheet2-accumulation.yaml` matches `~/planner.xlsx` Sheet2 inputs (single account, no contributions yet — Sheet2 starts from a current balance and a single rate)
- Test runs the projector and asserts each year-end balance deviation < 1%
- Failure produces a clear diff report so a real divergence is debuggable

**Points**: 3
**Traces to**: NFR-10, DISC-001 success criterion
