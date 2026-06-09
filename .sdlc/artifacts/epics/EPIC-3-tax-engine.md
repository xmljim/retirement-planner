# EPIC-3: Tax Engine

**Milestone**: M3 — Drawdown & Tax Engine
**Total points**: ~39
**Goal**: Full lifecycle projection with annual tax line: federal + 1 state, RMDs at 73/75, Roth conversions, taxable Social Security, with rollover events. Sheet2-equivalent inputs reproduce Sheet2 numbers within 1%.

## Stories

### S-3.1 — Federal brackets configuration loader
**As an** engine **I want** federal brackets, standard deduction, and SS-taxability thresholds loaded from year-keyed YAML **so that** annual tax-law updates don't require code changes.

**Acceptance criteria**
- `resources/data/federal-tax.yaml` populated for 2024–2026 published values; future years projected by `assumptions.bracketIndexationRate`
- Loader returns a `FederalTaxConfig` per year: brackets per filing status, standard deduction per filing status, SS provisional-income thresholds, qualified-dividend / LTCG brackets
- Tests cover: known year, projected year, single vs MFJ vs HoH

**Points**: 3
**Traces to**: FR-6.2, ADR-004

---

### S-3.2 — State tax loader with retirement-aware schema
**As an** engine **I want** state tax loaded for the user's state, including retirement-income subtractions where they materially change the bill **so that** the engine produces decision-grade tax estimates for retirees.

**Acceptance criteria**
- `resources/data/state-tax/{state}.yaml` for the chosen v1 state (TBD during M3 planning)
- Schema fields:
  - `model: BRACKETED | FLAT | NONE`
  - `brackets_<filing>` lists OR `flat_rate` (depending on model)
  - `standard_deduction.<filing>` (optional; absent means start from federal AGI)
  - `retirement_subtractions` block: `pension_annuity_under_55`, `pension_annuity_55_64`, `pension_annuity_65_plus` caps; `social_security_override` (FULL_EXEMPT | PARTIAL | FOLLOWS_FEDERAL); other named subtractions where relevant (e.g. NC Bailey, PA qualified-plan exclusion)
  - `source` URL + retrieval date in a comment header per ADR-004 audit policy
- Loader returns `StateTaxConfig`; `applySubtractions(stateAgi, person, year)` reduces state taxable income before applying brackets/flat rate
- Unsupported state throws `UnsupportedStateException` with a clear message
- Per ADR-004 "Fidelity Bar": tests verify directional correctness for representative retiree income mixes, not line-by-line return-grade accuracy
- Tests cover: bracket-state, flat-state, no-income-tax-state, retirement subtraction applied for ages 55–64 vs 65+, SS-fully-exempt state, capped subtraction (income exceeds the cap)

**Points**: 5
**Traces to**: FR-6.4, ADR-004

---

### S-3.3 — Provisional-income / taxable Social Security
**As an** engine **I want** to compute taxable Social Security via the IRS provisional-income formula **so that** retirees with mixed income have correct AGI.

**Acceptance criteria**
- `computeTaxableSocialSecurity(grossSS, agiExcludingSS, taxExemptInterest, filing, year)` returns `Money taxableAmount`
- Implements the two-tier IRS formula (0% / 50% / 85%)
- Tests cover: below first threshold (0% taxable), between thresholds (50%), above second (85%), exact threshold boundaries

**Points**: 3
**Traces to**: FR-6.3, ADR-004

---

### S-3.4 — TaxEngine: computeYearly skeleton
**As an** engine **I want** a `TaxEngine.computeYearly(TaxYearInputs)` returning a `TaxResult` **so that** simulation can call it once per year.

**Acceptance criteria**
- Inputs and result records as defined in ADR-004
- Computes: gross income, AGI, taxable SS (via S-3.3), taxable income, federal tax (brackets + std deduction), state tax, totals, effective rate, marginal rate
- Marginal rate is the rate of the highest bracket the taxable income reached
- Tests round-trip a small set of known returns from public sources (e.g. canonical W-2 + IRA withdrawal cases)

**Points**: 5
**Traces to**: FR-6.1, ADR-004

---

### S-3.5 — IRS Uniform Lifetime Table loader
**As an** engine **I want** the IRS Uniform Lifetime Table loaded as config **so that** RMD divisors are accurate and updatable.

**Acceptance criteria**
- `resources/data/rmd-uniform-lifetime.yaml` carries age → divisor for the published table
- Loader returns divisor for any age in range; out-of-range throws
- Birth-year → RMD-start-age mapping (73 vs 75 per SECURE 2.0) configured separately
- Tests cover: known ages, boundary ages

**Points**: 2
**Traces to**: FR-6.5, ADR-004

---

### S-3.6 — RMD enforcement
**As an** engine **I want** RMDs computed and forced when user withdrawals fall short **so that** simulations remain compliant.

**Acceptance criteria**
- For each Trad IRA / Trad 401k account at a Person ≥ RMD start age: `required = priorYearEndBalance / divisor[age]`
- If user-planned withdrawals from that account < required: engine forces additional withdrawal of the gap, adds to ordinary income
- Roth IRA and post-2024 Roth 401k excluded
- If account would drain below zero: drain to zero, emit `RmdShortfall` warning, simulation continues
- Tests cover: RMD met by user, RMD shortfall (forced top-up), RMD shortfall with insufficient balance (warning), Roth account excluded

**Points**: 5
**Traces to**: FR-6.5, FR-6.6, ADR-004

---

### S-3.7 — RolloverEvent processing
**As a** user **I want** to define rollover events with effective date, source, destination, and amount **so that** mid-plan account moves are modeled.

**Acceptance criteria**
- `RolloverEvent` entity (date, source, destination, amount mode: FullBalance | Fixed | Percent)
- Engine processes events in date order at the month boundary
- Same-treatment moves (Trad → Trad IRA): no tax event
- Cross-treatment moves (Trad → Roth): triggers Roth conversion logic (S-3.8)
- Tests: 401k → IRA at retirement (no tax), partial rollover, percent rollover

**Points**: 3
**Traces to**: FR-4.1, FR-4.3, ADR-004

---

### S-3.8 — Roth conversions
**As a** user **I want** to plan Roth conversions with fixed amount or fill-bracket mode **so that** I can model conversion ladders during bridge years.

**Acceptance criteria**
- `RothConversion` entity: tax year, source Trad account, destination Roth account, `ConversionAmount` (FIXED | FILL_BRACKET(targetMarginalRate))
- `FILL_BRACKET` solves for the conversion amount that brings taxable income to the top of the targeted bracket
- Conversion taxed as ordinary income in the conversion year
- Tests cover: fixed conversion amount, fill-12% bracket from a low-income bridge year, fill-bracket when already above target (zero conversion)

**Points**: 5
**Traces to**: FR-4.2, FR-4.3, ADR-004

---

### S-3.9 — Year-end tax accounting and payment
**As an** engine **I want** the annual tax computed and debited from a configured payment source **so that** post-tax balances reflect reality.

**Acceptance criteria**
- At each tax-year boundary: compute `TaxResult` from cash flows since prior year-end
- December cash flow line `TAX_PAYMENT` debits the configured tax-source account (default: taxable brokerage; configurable)
- If tax-source account insufficient: emits `TaxShortfall` warning, simulation continues with negative cash recorded
- Tests cover: simple year, year with conversion, year with RMD top-up, year with insufficient tax source

**Points**: 3
**Traces to**: FR-6.7, ADR-004

---

### S-3.10 — Drawdown projection orchestrator
**As a** user **I want** the projection extended through bridge and drawdown phases **so that** I see the full lifecycle to end-of-plan.

**Acceptance criteria**
- `simulation/LifecycleProjector` orchestrates accumulation → bridge → drawdown phases
- Default end-of-plan: primary person age 95 (configurable per Plan)
- Bridge phase: SS not yet started; healthcare cost as a configured monthly debit
- Drawdown phase: SS active, RMDs enforced from age 73/75
- REST endpoint `GET /plans/{id}/projection?mode=deterministic` extended to include all phases
- Tests: full-lifecycle fixture matches a hand-checked annuity calculation within 1%

**Points**: 5
**Traces to**: FR-7.1, FR-7.2, NFR-10

---

### S-3.11 — Sheet2 full-lifecycle fidelity fixture
**As a** maintainer **I want** an integration test asserting the engine reproduces Sheet2 outputs within 1% on matched inputs across all phases **so that** the migration from spreadsheet to app is defensible.

**Acceptance criteria**
- Fixture matches Sheet2 inputs end-to-end (DOB, balance, retirement date, SS election, healthcare, etc.)
- Test asserts month-by-month bridge balance and retirement balance within 1% deviation
- Test runs in < 10s (deterministic, no MC)
- Includes a Sheet2 limitations note: Sheet2 has no tax math, so this fixture configures `TaxEngine` with all brackets at 0% and no RMD start ages — proving the engine's *projection arithmetic* matches Sheet2 when the tax layer is bypassed

**Points**: 3
**Traces to**: NFR-10, DISC-001 success criterion
