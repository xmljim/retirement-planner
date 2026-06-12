-- Copyright (c) 2026 Jim Earley. All rights reserved.
-- Licensed under PolyForm Noncommercial 1.0.0 plus the project's
-- AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
--
-- V4: ContributionPolicy on the Account aggregate per ADR-003 / FR-2.4.
--
-- Sealed ContributionAmount stored as discriminator + JSONB payload
-- (mirrors the SleeveKind/SleeveYieldPolicy pattern from V3). The
-- non-sealed records (EscalationPolicy, EmployerMatch) live in single
-- nullable JSONB columns — NULL means absent.
--
-- Adds TRADITIONAL_403B and ROTH_403B to the account_type domain so
-- employer-sponsored 403(b) plans (the public-school / nonprofit
-- analogue of 401(k)) can carry contribution policies under the same
-- match-validity rules.
--
-- A CHECK constraint enforces that an employer match is only attached
-- to a 401(k) or 403(b) variant; the domain record validates the same
-- invariant in-process.

ALTER TABLE account
    DROP CONSTRAINT account_account_type_check;

ALTER TABLE account
    ADD CONSTRAINT account_account_type_check CHECK (account_type IN (
        'TRADITIONAL_401K',
        'ROTH_401K',
        'TRADITIONAL_403B',
        'ROTH_403B',
        'TRADITIONAL_IRA',
        'ROTH_IRA',
        'HSA',
        'TAXABLE_BROKERAGE',
        'CASH',
        'PENSION'
    ));

ALTER TABLE account
    ADD COLUMN contribution_amount_type   TEXT,
    ADD COLUMN contribution_amount_data   JSONB,
    ADD COLUMN escalation_data            JSONB,
    ADD COLUMN employer_match_data        JSONB,
    ADD COLUMN contribution_start_date    DATE,
    ADD COLUMN contribution_end_date      DATE;

ALTER TABLE account
    ADD CONSTRAINT account_contribution_amount_type_check CHECK (
        contribution_amount_type IS NULL
        OR contribution_amount_type IN ('PERCENT_OF_SALARY', 'FIXED_DOLLAR')
    );

-- The discriminator and the data column are coupled: both null (no
-- policy) or both non-null (policy present, including its variant data).
ALTER TABLE account
    ADD CONSTRAINT account_contribution_amount_pairing CHECK (
        (contribution_amount_type IS NULL AND contribution_amount_data IS NULL)
        OR (contribution_amount_type IS NOT NULL AND contribution_amount_data IS NOT NULL)
    );

-- Escalation and match presuppose an employee amount.
ALTER TABLE account
    ADD CONSTRAINT account_escalation_requires_amount CHECK (
        escalation_data IS NULL OR contribution_amount_type IS NOT NULL
    );

ALTER TABLE account
    ADD CONSTRAINT account_match_requires_amount CHECK (
        employer_match_data IS NULL OR contribution_amount_type IS NOT NULL
    );

-- Employer match is only valid on 401(k) / 403(b) variants per ADR-003.
ALTER TABLE account
    ADD CONSTRAINT account_match_account_type CHECK (
        employer_match_data IS NULL
        OR account_type IN ('TRADITIONAL_401K', 'ROTH_401K', 'TRADITIONAL_403B', 'ROTH_403B')
    );

-- If both dates are set, end must not precede start.
ALTER TABLE account
    ADD CONSTRAINT account_contribution_date_order CHECK (
        contribution_start_date IS NULL
        OR contribution_end_date IS NULL
        OR contribution_end_date >= contribution_start_date
    );
