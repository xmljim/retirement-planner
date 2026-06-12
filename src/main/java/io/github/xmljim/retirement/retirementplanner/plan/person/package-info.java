/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Person aggregate within the Plan module: {@code Person},
 * {@code PersonId}, repository / service interfaces. Persistence lives
 * in {@code internal/}.
 *
 * <p>Exposed as a {@link org.springframework.modulith.NamedInterface}
 * so {@code api/}, {@code contribution/}, and {@code tax/} can resolve
 * persons referenced from accounts and tax filings.
 */
@NamedInterface("person")
package io.github.xmljim.retirement.retirementplanner.plan.person;

import org.springframework.modulith.NamedInterface;
