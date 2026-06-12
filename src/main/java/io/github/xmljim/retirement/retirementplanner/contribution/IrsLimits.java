/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution;

import java.util.Objects;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * IRS contribution limits and SECURE 2.0 thresholds for a single
 * calendar year (ADR-003).
 *
 * <p>All amounts are USD per ADR-007. Catch-up amounts are the
 * <em>additional</em> dollars allowed on top of the base, matching how
 * the IRS publishes them.
 *
 * <p>{@link Source#PUBLISHED} values come from {@code irs-limits.yaml}
 * and trace to the IRS notice for that year. {@link Source#PROJECTED}
 * values are the engine's forward extrapolation when no published year
 * is available.
 *
 * @param year                              the calendar year these limits apply to
 * @param employee401kBase                  §402(g) elective-deferral base limit (401(k)/403(b))
 * @param employee401k50PlusCatchup         §414(v) age 50+ additional catch-up
 * @param employee401k60PlusCatchup         SECURE 2.0 §109 ages 60–63 super catch-up
 * @param iraBase                           §408(b)/(p)(2)(C) IRA base (Traditional + Roth combined)
 * @param ira50PlusCatchup                  IRA age 50+ additional catch-up
 * @param hsaSelfOnly                       §223 HSA self-only base
 * @param hsaFamily                         §223 HSA family base
 * @param hsa55PlusCatchup                  §223 age 55+ additional catch-up
 * @param totalDc                           §415(c) total defined-contribution plan limit (employee + employer combined)
 * @param secure2_0_603HighEarnerThreshold  §603 prior-year FICA wage threshold above which catch-up must be Roth
 * @param source                            whether these limits are published or projected forward
 */
public record IrsLimits(
        int year,
        Money employee401kBase,
        Money employee401k50PlusCatchup,
        Money employee401k60PlusCatchup,
        Money iraBase,
        Money ira50PlusCatchup,
        Money hsaSelfOnly,
        Money hsaFamily,
        Money hsa55PlusCatchup,
        Money totalDc,
        Money secure2_0_603HighEarnerThreshold,
        Source source) {

    public IrsLimits {
        Objects.requireNonNull(employee401kBase, "employee401kBase");
        Objects.requireNonNull(employee401k50PlusCatchup, "employee401k50PlusCatchup");
        Objects.requireNonNull(employee401k60PlusCatchup, "employee401k60PlusCatchup");
        Objects.requireNonNull(iraBase, "iraBase");
        Objects.requireNonNull(ira50PlusCatchup, "ira50PlusCatchup");
        Objects.requireNonNull(hsaSelfOnly, "hsaSelfOnly");
        Objects.requireNonNull(hsaFamily, "hsaFamily");
        Objects.requireNonNull(hsa55PlusCatchup, "hsa55PlusCatchup");
        Objects.requireNonNull(totalDc, "totalDc");
        Objects.requireNonNull(secure2_0_603HighEarnerThreshold, "secure2_0_603HighEarnerThreshold");
        Objects.requireNonNull(source, "source");
    }

    /** Provenance of an {@link IrsLimits} record. */
    public enum Source {
        /** Loaded directly from {@code irs-limits.yaml} for a year the IRS has published. */
        PUBLISHED,
        /** Computed from the latest published year by applying the configured growth rate. */
        PROJECTED
    }
}
