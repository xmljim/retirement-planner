/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/** Pre-cap planned employee contribution for one account in one month. */
record PlannedContribution(Account account, Money intended) {}
