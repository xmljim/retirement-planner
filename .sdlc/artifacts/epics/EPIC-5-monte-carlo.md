# EPIC-5: Monte Carlo & Returns

**Milestone**: M5 — Monte Carlo
**Total points**: ~35
**Goal**: 1000 simulations × 50-year plan in < 5 seconds. Output Parquet contains percentile bands and per-bucket sacrifice statistics.

## Stories

### S-5.1 — Historical returns Parquet datasets
**As an** engine **I want** historical monthly returns bundled per asset class **so that** Monte Carlo has data to sample from.

**Acceptance criteria**
- Bundled Parquet files at `resources/data/returns/<asset_class>.parquet` for: US Large Cap, US Small Cap, International Developed, Emerging Markets, US Aggregate Bonds, US TIPS, Cash
- Schema: `date` (yyyy-MM-01), `return_pct` (BigDecimal), `source` metadata
- Data sources documented in `resources/data/returns/README.md` (Shiller for S&P 500, etc.)
- Loader returns asset-class returns as a `double[]` array (per ADR-005 dual-precision approach)
- Tests verify each dataset's row count and date contiguity

**Points**: 5
**Traces to**: FR-5.4, ADR-005

---

### S-5.2 — BlobStore interface and local filesystem implementation
**As a** developer **I want** a `BlobStore` interface with a local filesystem implementation **so that** Parquet reads/writes are abstracted from cloud-specific code.

**Acceptance criteria**
- Interface in `shared/` (or a dedicated `storage/` module — decide during design): `put`, `get`, `exists`, `list(prefix)`, `delete(prefix)`
- Local impl writes under `${app.data.dir}/blobs/` with the path conventions from ADR-006
- Read-only access to bundled Parquet datasets (no put allowed in `data/returns/` prefix)
- Tests cover: round-trip put/get, list, delete-prefix, read-only-prefix enforcement

**Points**: 3
**Traces to**: ADR-006

---

### S-5.3 — AssetAllocationPolicy and glide path
**As a** user **I want** to define a glide path of `(referenceAge, weights)` points **so that** allocation shifts naturally over time.

**Acceptance criteria**
- `AssetAllocationPolicy` value record with `List<GlidePathPoint>`
- `weightsAt(age)` linearly interpolates between bracketing points
- Validation: weights sum to 1.0 ± 0.001 at each point; asset classes must match the bundled set
- Per-Plan attached
- Tests cover: single point (constant), two points (interpolation), boundary (before first / after last), zero weight on an asset class

**Points**: 3
**Traces to**: FR-5.2, FR-5.3, ADR-005

---

### S-5.4 — Block bootstrap sampler
**As an** engine **I want** to sample contiguous blocks of multi-asset returns **so that** Monte Carlo preserves autocorrelation and cross-asset correlations.

**Acceptance criteria**
- `BlockBootstrap.sample(seed, blockLengthMonths, totalMonths) → double[][]` returns a months-by-assetClasses matrix
- Same seed + same params + same dataset = identical output
- Default block length 12 months; configurable per scenario
- All asset classes sampled in lockstep (same blocks across all classes — preserves correlation)
- Sampling beyond the available history wraps deterministically rather than failing
- Tests cover: reproducibility, block length, cross-asset alignment, edge case where horizon > history

**Points**: 5
**Traces to**: FR-7.4, ADR-005

---

### S-5.5 — Money ↔ double conversion utility (MoneyDoubleBridge)
**As an** engine **I want** a tested utility for converting Money to double and back **so that** the dual-precision boundary is bounded and verifiable.

**Acceptance criteria**
- `MoneyDoubleBridge` in `simulation.montecarlo.internal/`: `toDouble(Money)`, `fromDouble(double, Currency)`, `accumulate(double[], double, idx)`
- Round-trip identity verified for typical ranges; documented absolute and relative tolerances
- JMH benchmark verifies the inner-loop cost in the budget
- Comment on the class explicitly references ADR-007 sanctioning the exception

**Points**: 3
**Traces to**: ADR-007, ADR-005

---

### S-5.6 — Monte Carlo single-simulation runner
**As an** engine **I want** to execute one full simulation given a seed and a sampled return path **so that** N simulations can be parallelized.

**Acceptance criteria**
- `MonteCarloRunner.runSingle(plan, seed, returnPath) → SimulationResult`
- Inner loop in `double` per ADR-005 / ADR-007; year-end converts to BigDecimal for tax/RMD/bucket evaluation
- Glide path evaluated each month
- All ADR-002 / ADR-003 / ADR-004 logic active (contributions, tax, RMDs, buckets, lifecycle, reallocation, conversions)
- Output: end-of-plan portfolio value, year-of-failure (if any, defined as: any required spend not met from any source), per-year trajectory (yearly snapshots), per-bucket spending decision history

**Points**: 5
**Traces to**: FR-7.3, FR-7.5, FR-7.6, ADR-005

---

### S-5.7 — Parallel Monte Carlo orchestrator
**As an** engine **I want** to run N simulations in parallel and aggregate results **so that** the 5-second performance target is met.

**Acceptance criteria**
- `MonteCarloEngine.run(plan, runConfig) → MonteCarloResult`
- Parallel streams across simulations with per-thread `Random` instance
- Aggregations: success probability, percentile bands (10/25/50/75/90), worst-case trajectory, **bucket-sacrifice statistics** ("Travel scaled ≥20% in X% of sims; Legacy missed in Y%")
- JMH benchmark in CI verifies 1000-sim × 50-year plan completes in < 5s on a reference dev machine
- Test asserts determinism: same seed reproduces identical aggregates

**Points**: 5
**Traces to**: FR-7.4, FR-7.7, FR-9.4, NFR-1, ADR-005

---

### S-5.8 — MonteCarloResult Parquet output
**As a** developer **I want** Monte Carlo results written as Parquet via BlobStore **so that** results are persisted and analyzable.

**Acceptance criteria**
- One Parquet file per run at `tenants/{tenantId}/scenarios/{scenarioId}/runs/{runId}.parquet`
- Schema documented in code: per-simulation rows + aggregated summary rows
- Read API: `RunResultReader.read(runId) → MonteCarloResult` reproduces the in-memory shape from the file
- Tests cover: write/read round-trip, schema versioning bytes (the file carries a schema-version field)

**Points**: 3
**Traces to**: FR-9.4, ADR-006

---

### S-5.9 — REST endpoint to start a run and fetch results
**As a** user **I want** to start a Monte Carlo run via API and retrieve its results **so that** the frontend can drive runs.

**Acceptance criteria**
- `POST /scenarios/{id}/runs` starts a run with `RunConfiguration` (seed optional — server defaults; sim count; block length)
- Response includes `runId` and a status URL
- `GET /runs/{id}` returns aggregated results JSON (percentile bands, success probability, sacrifice stats)
- `GET /runs/{id}/raw` returns a download of the per-simulation Parquet (for power users)
- OpenAPI updated; frontend types regenerate cleanly
- Tests cover: sync-OK case (small sim count), large-run async case (returns 202 with status URL — full async is implemented in EPIC-6)

**Points**: 3
**Traces to**: FR-7.3, FR-7.8

---

### S-5.10 — Glide-path enforcement under MC
**As an** engine **I want** glide-path target weights enforced each month under Monte Carlo **so that** asset allocation continuously rebalances.

**Acceptance criteria**
- Each month: target weights computed; sleeve balances rebalanced to those weights before applying that month's returns
- Cash sleeves outside the asset-allocation glide path keep their explicit policy (CASH SleeveKind is excluded from glide-path rebalancing)
- Tests verify: 80/20 → 60/40 over 30 years lands at the configured weights, cash sleeve doesn't drift, returns applied to post-rebalance balances

**Points**: 3
**Traces to**: FR-7.5, ADR-005
