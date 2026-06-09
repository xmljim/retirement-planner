# EPIC-4: Buckets & Adaptive Spending

**Milestone**: M4 — Buckets
**Total points**: ~50
**Goal**: API supports defining buckets with funding/spending/lifecycle policies. Projection includes per-bucket cash flows, sleeve-aware reallocation, end-of-life sweeps, and adaptive spending decisions per year.

## Stories

### S-4.1 — Bucket sealed interface and registry
**As a** developer **I want** a `Bucket` sealed interface plus a `BucketRegistry` **so that** new bucket types can be added without engine changes.

**Acceptance criteria**
- `Bucket` sealed interface in `plan/` per ADR-002 (interface/value-types live with the aggregate; engine in `bucket/`)
- `BucketRegistry` holds factory functions per type
- Extension test: a fake `BucketBucketExt` type registered in test scope works end-to-end without changes to the engine
- ApplicationModules verification still passes

**Points**: 3
**Traces to**: FR-3.1, FR-3.8, ADR-002

---

### S-4.2 — FundingPolicy implementations
**As a** user **I want** to choose how a bucket is funded (PrefundAtRetirement, SinkingFund, PullOnDemand, Constraint) **so that** different goals match different cash strategies.

**Acceptance criteria**
- Sealed `FundingPolicy` with all four implementations
- `PrefundAtRetirement(targetAtRetirement)` — engine moves cash from retirement accounts to the bucket's host sleeve at retirement date
- `SinkingFund(monthlyContribution, sourceAccount)` — engine accrues monthly during accumulation
- `PullOnDemand(sourceAccount)` — bucket has no balance; spending pulls from source at the moment of draw
- `Constraint(minimumAtEndOfPlan)` — no cash flow; influences success metric
- Tests cover each policy's per-month behavior and edge cases (e.g. SinkingFund whose accumulation isn't enough by goal date)

**Points**: 5
**Traces to**: FR-3.2, FR-3.5, ADR-002

---

### S-4.3 — Built-in bucket types
**As a** user **I want** ready-made bucket types for the common goals **so that** I don't have to specify everything from scratch.

**Acceptance criteria**
- `BridgeBucket` — defaults: PrefundAtRetirement; SpendingPolicy=Fixed; priority=1; LifecyclePolicy=SweepTo(GeneralSpending) at SS start
- `RecurringAnnualBucket(startAge, endAge, annualAmount)` — defaults: SinkingFund; SpendingPolicy=AgeBasedDecline; priority=10; LifecyclePolicy=Retain
- `OneTimeGoalBucket(targetDate, amount)` — defaults: SinkingFund; LifecyclePolicy=SweepToAccount(taxable)
- `HealthcareBucket(monthlyAmount, ageIndexedCurve)` — defaults: SinkingFund; SpendingPolicy=Fixed (the curve already encodes age increase); priority=2
- `LegacyTargetBucket(targetAtEndOfPlan)` — Constraint funding; no cash flows; priority=99 (lowest)
- Per-type unit tests confirm defaults and override behavior

**Points**: 5
**Traces to**: FR-3.1, FR-3.2, ADR-002

---

### S-4.4 — Bucket hosting (sleeve-hosted, account-hosted, virtual)
**As a** user **I want** a bucket to be hosted on a specific sleeve, a whole account, or be virtual **so that** the model handles IRA cash sleeves correctly.

**Acceptance criteria**
- `BucketHosting` value record with three variants matching ADR-002
- Validation: PrefundAtRetirement and SinkingFund require a sleeve-hosted or account-hosted bucket; PullOnDemand requires virtual
- Persistence: hosting reference stored on the Bucket entity
- Tests cover: BridgeBucket on cash sleeve of Trad IRA (validates that host yield ≠ wrapper tax treatment), Travel bucket on taxable, virtual PullOnDemand bucket

**Points**: 3
**Traces to**: FR-3.5, ADR-002

---

### S-4.5 — SpendingPolicy core implementations
**As a** user **I want** the standard spending policies (Fixed, AcrossTheBoardReduction, PriorityFirst, InflationFloor, AgeBasedDecline) **so that** the engine can react to market and life conditions.

**Acceptance criteria**
- All five policies implement `SpendingPolicy.evaluate(BucketState, PortfolioState, MarketState, yearIndex) → SpendingDecision(scaleFactor, defer, rationale)`
- `Fixed` always returns 1.0
- `AcrossTheBoardReduction(threshold, factor)` returns `factor` when portfolio < threshold, else 1.0
- `PriorityFirst` — preserves higher-priority buckets at 1.0; scales lower-priority by computed factor based on current portfolio shortfall
- `InflationFloor(realDollarFloor)` clamps the result of any composed prior policy to ≥ floor
- `AgeBasedDecline(annualRealDeclineRate, Optional<LateLifeUptick>)` — applies cumulative decline; respects late-life uptick when configured
- `rationale` strings populated and human-readable
- Per-policy unit tests cover boundary cases

**Points**: 5
**Traces to**: FR-3.3, ADR-002

---

### S-4.6 — Advanced SpendingPolicies
**As a** user **I want** GuytonKlingerGuardrail and DeferredSink policies **so that** I can model classical dynamic-withdrawal strategies.

**Acceptance criteria**
- `GuytonKlingerGuardrail(ceiling, floor, adjustmentPct)` evaluates current effective withdrawal rate against ceiling/floor; cuts/raises by adjustmentPct
- `DeferredSink(marketTrailingThreshold)` — when trailing-12-month return below threshold, returns `defer=true`; carried-forward balance accrues on the bucket and is eligible next year
- `Composite(List<SpendingPolicy>)` applies in order; later policies see the prior decision
- Tests cover: GK ceiling firing, GK floor firing, DeferredSink in down market, DeferredSink reactivating in following year, Composite of [AgeBasedDecline, PriorityFirst, InflationFloor]

**Points**: 5
**Traces to**: FR-3.3, ADR-002

---

### S-4.7 — LifecyclePolicy implementations
**As an** engine **I want** to sweep leftover bucket balance at end-of-life per a LifecyclePolicy **so that** unused funds don't strand.

**Acceptance criteria**
- Sealed `LifecycleAction`: SweepTo(BucketRef), SweepToAccount(AccountRef), SweepProportional(List<BucketRef>), Retain
- End-of-life triggered per bucket type (Bridge=SS start; RecurringAnnual=end of age window; OneTime=end of goal year; Healthcare/Legacy=end of plan)
- Sweep produces a `BUCKET_SWEEP` cash flow line, applies tax rules per the host accounts
- Tests cover each action; cross-account sweep that triggers a Roth conversion is verified to invoke the conversion path

**Points**: 5
**Traces to**: FR-3.6, ADR-002

---

### S-4.8 — BucketReallocationEvent
**As a** user **I want** to define mid-plan reallocation events with FullBalance/Fixed/Percent/ExcessOver modes **so that** I can rebalance bucket allocations.

**Acceptance criteria**
- `BucketReallocationEvent` entity with all four amount modes
- Engine processes events in date order alongside RolloverEvent and RothConversion
- Same-sleeve: pure sub-balance reassignment, no tax
- Cross-sleeve same-account taxable: triggers gain realization via tax engine
- Cross-account: routes through rollover/conversion tax rules
- Tests cover all three host scenarios and the EXCESS_OVER edge cases (over-reserve, exactly-reserve, under-reserve)

**Points**: 5
**Traces to**: FR-3.7, ADR-002

---

### S-4.9 — Bucket priority-ordered draw step
**As an** engine **I want** to draw from buckets in priority order with policy-applied scaling each year **so that** spending realistically responds to portfolio conditions.

**Acceptance criteria**
- At each tax-year boundary, in `simulation/`: for each bucket sorted by priority ascending, compute `targetSpend → SpendingDecision → actualSpend`; record `BUCKET_DRAW` cash flows
- Deferred amounts carried forward on the bucket for next-year evaluation
- `SpendingDecision`s recorded per bucket per year for reporting (consumed by EPIC-5)
- Tests cover: priority-first scaling under stress, deferral and carry-forward, mixed buckets with mixed policies

**Points**: 5
**Traces to**: FR-3.4, ADR-002

---

### S-4.10 — Healthcare bucket age-indexed curve
**As a** user **I want** the healthcare bucket to follow an age-indexed cost curve **so that** late-life healthcare reflects empirical reality.

**Acceptance criteria**
- `HealthcareBucket` accepts an `AgeCostCurve` (list of `(age, monthlyCost)` points; linear interpolation between)
- Bundled default curve loosely modeled on Fidelity / Genworth public estimates (citation in resource file)
- Curve overrideable per Plan
- Tests verify the bucket's monthly draw at ages 65, 75, 85 matches the configured curve

**Points**: 3
**Traces to**: FR-3.1, FR-9.3, ADR-002

---

### S-4.11 — Bucket-aware projection extension
**As a** user **I want** the projection to reflect bucket draws, sweeps, and reallocations **so that** I see goal-based spending in context.

**Acceptance criteria**
- `LifecycleProjector` (from EPIC-3) extended to call bucket steps at year boundaries
- `MonthlyProjection` rows include per-bucket lines (target, actual, scale factor, rationale)
- REST projection endpoint returns the enriched output
- Performance: deterministic 50-year projection with 5 buckets completes in < 500ms (early performance check before MC)

**Points**: 5
**Traces to**: FR-9.3, NFR-1

---

### S-4.12 — Cross-account reallocation tax-correctness matrix
**As a** maintainer **I want** an explicit unit-test matrix covering reallocation tax interactions **so that** the trickiest cases are nailed down.

**Acceptance criteria**
- Test matrix covers: same-sleeve reallocation (no tax), cross-sleeve in tax-advantaged (no tax), cross-sleeve in taxable (gain realized), cross-account Trad→Trad IRA (no tax), cross-account Trad→Roth IRA (conversion taxed)
- Each case has a fixture, expected ledger lines, and an assertion
- Matrix lives in `bucket/internal/test/ReallocationTaxMatrixTest.java`

**Points**: 3
**Traces to**: FR-3.7, ADR-002, ADR-004
