-- Copyright (c) 2026 Jim Earley. All rights reserved.
-- Licensed under PolyForm Noncommercial 1.0.0 plus the project's
-- AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
--
-- V3: Account aggregate with one or more AccountSleeves per ADR-002.
--
-- Tax treatment is data on AccountType (not subclasses). Owner is a
-- Person (Individual) or JOINT — modeled with a TEXT discriminator
-- + nullable owner_person_id FK and a CHECK enforcing the pairing.
--
-- Sleeves carry a Money balance (NUMERIC(19,6) per ADR-007), a sleeve
-- kind discriminator + JSONB payload (only FixedAllocation populates
-- weights), and a yield policy with the same discriminator+JSONB
-- shape so that future variants don't require a schema change.
--
-- Cascade chain: deleting a Plan cascades to its Accounts, and
-- deleting an Account cascades to its Sleeves.

CREATE TABLE account (
    id                BIGSERIAL PRIMARY KEY,
    plan_id           BIGINT NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    account_type      TEXT NOT NULL CHECK (account_type IN (
        'TRADITIONAL_401K',
        'ROTH_401K',
        'TRADITIONAL_IRA',
        'ROTH_IRA',
        'HSA',
        'TAXABLE_BROKERAGE',
        'CASH',
        'PENSION'
    )),
    owner_type        TEXT NOT NULL CHECK (owner_type IN ('INDIVIDUAL', 'JOINT')),
    owner_person_id   BIGINT REFERENCES person (id) ON DELETE RESTRICT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT account_owner_pairing CHECK (
        (owner_type = 'INDIVIDUAL' AND owner_person_id IS NOT NULL)
        OR (owner_type = 'JOINT' AND owner_person_id IS NULL)
    )
);

CREATE INDEX idx_account_plan ON account (plan_id);
CREATE INDEX idx_account_owner_person ON account (owner_person_id) WHERE owner_person_id IS NOT NULL;

CREATE TABLE account_sleeve (
    id                BIGSERIAL PRIMARY KEY,
    account_id        BIGINT NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    kind_type         TEXT NOT NULL CHECK (kind_type IN ('CASH', 'ASSET_ALLOCATION', 'FIXED_ALLOCATION')),
    kind_data         JSONB,
    yield_type        TEXT NOT NULL CHECK (yield_type IN ('FIXED_RATE', 'MONEY_MARKET', 'TRACKS_ALLOCATION')),
    yield_data        JSONB,
    balance_amount    NUMERIC(19, 6) NOT NULL,
    balance_currency  VARCHAR(3) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT sleeve_kind_data_required CHECK (
        (kind_type = 'FIXED_ALLOCATION' AND kind_data IS NOT NULL)
        OR (kind_type IN ('CASH', 'ASSET_ALLOCATION'))
    ),
    CONSTRAINT sleeve_yield_data_required CHECK (
        (yield_type = 'FIXED_RATE' AND yield_data IS NOT NULL)
        OR (yield_type IN ('MONEY_MARKET', 'TRACKS_ALLOCATION'))
    )
);

CREATE INDEX idx_account_sleeve_account ON account_sleeve (account_id);
