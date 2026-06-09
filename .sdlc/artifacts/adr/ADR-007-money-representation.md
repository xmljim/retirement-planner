# ADR-007: Money Representation & Precision

- **Status**: Accepted
- **Date**: 2026-06-08
- **Deciders**: Owner
- **Related**: DISC-001, ADR-005 (MC inner-loop exception)

## Context

Money math in `double` is the canonical "billion-dollar mistake" of
financial software. The retirement planner deals with multi-decade
projections, tax bracket arithmetic, percentage contributions, and
display values. We need a single rule, applied consistently.

## Decision

### Internal Representation
All monetary values use `java.math.BigDecimal`, wrapped in a `Money`
value type:

```java
record Money(BigDecimal amount, Currency currency) {
    public static final Money ZERO_USD = of(BigDecimal.ZERO, Currency.getInstance("USD"));

    public static Money of(BigDecimal amount, Currency currency) { ... }
    public static Money usd(String amount) { return of(new BigDecimal(amount), USD); }

    public Money plus(Money other) { /* assert same currency */ }
    public Money minus(Money other) { ... }
    public Money times(BigDecimal factor) { ... }
    public Money dividedBy(BigDecimal divisor) { ... }
    // ...
}
```

`Money` is the type used in all domain entities, value records, and DTOs.
Strings construct from a string literal to avoid `double → BigDecimal`
precision loss.

### Internal Scale
`BigDecimal` arithmetic uses **scale 6** internally and **rounding mode
`HALF_EVEN`** (banker's rounding) for any rounding step. Scale 6 is
enough to keep accumulated rounding error well below a cent over 50-year
projections; HALF_EVEN minimizes systematic bias.

### Display Scale
The boundary to UI uses **scale 2** for dollars. Conversion happens at
the DTO mapping layer; domain code never rounds for display.

### Percentage / Rate Values
Rates (interest, inflation, contribution percentages, tax rates) use
plain `BigDecimal` — *not* `Money`. Convention: rates are stored as
decimals (0.0245 for 2.45%, not 2.45). Percentage display is a
view-layer concern.

### Currency
Single-currency at v1 (USD only). The `Money` type carries a `Currency`
field anyway — refusing to silently strip it now means multi-currency
later isn't a refactor. Cross-currency operations throw.

### JPA / JSON
- JPA: `Money` mapped via `@Embeddable` → two columns
  (`{field}_amount NUMERIC(19,6)`, `{field}_currency CHAR(3)`).
  Migration policy: every monetary column ships with both fields.
- JSON: serialized as `{"amount":"12345.67","currency":"USD"}` with
  `amount` as a string to avoid JS-number precision loss.

### The Monte Carlo Exception
ADR-005 specifies that the Monte Carlo inner loop converts to `double`
for performance (1000 sims × 600 months exceeds the BigDecimal budget).
**This exception is bounded:**
- Only the per-month return-multiplication step uses `double`.
- Year-end balances, tax computations, RMDs, and any user-visible
  output convert back to `BigDecimal` before further use.
- The conversion boundary is a single utility (`MoneyDoubleBridge`)
  with thorough tests verifying round-trip stability and that
  accumulated double error stays below a defined tolerance per
  simulation.

No other code path is permitted to use `double` for money.

## Rationale

- **`Money` value type** prevents accidental currency mixing and gives
  us one obvious place to put arithmetic helpers and validations.
- **Scale 6 internal / scale 2 display** is a defensible choice that
  matches common practice in financial software — see, e.g., the JSR
  354 `MonetaryAmount` defaults.
- **HALF_EVEN** avoids the rounding bias that can compound over decades
  with HALF_UP.
- **Decimal rates (not percentages)** removes a constant trap of
  multiplying by 100 in the wrong place.
- **String JSON serialization for amount** prevents JS clients from
  silently truncating large numbers via JS `Number`.

## Consequences

**Positive**
- One rule, mechanically enforceable (Checkstyle/PMD can flag `double`
  in non-MC code).
- Database column types are predictable.
- Display rounding is centralized.

**Negative**
- Slightly verbose code (`money.plus(other)` vs. `a + b`).
- BigDecimal is not free; matters for the MC inner loop, hence the bounded exception.
- Embeddable two-column mapping for every Money field adds DB columns;
  acceptable.

## Alternatives Considered

- **Use `long` cents** — rejected; rates and intermediate calculations
  still need decimals, and overflow risk exists at multi-decade scales
  with very large balances.
- **JSR 354 (`javax.money`)** — viable but heavier than needed for this
  app. May revisit if we add real multi-currency.
- **Use `double` everywhere** — rejected on principle.

## Notes

- A custom Checkstyle rule should flag `double` and `float` in any
  package outside `simulation.montecarlo.internal` — to be added with
  the build pipeline ADR/skill.
- `Money.times(BigDecimal)` returns `Money` with the original currency;
  rounding to internal scale happens here.
- A small JMH benchmark to verify the BigDecimal budget claim (or
  motivate the MC exception) lives in `src/test/jmh/`.
