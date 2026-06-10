-- Copyright (c) 2026 Jim Earley. All rights reserved.
-- Licensed under PolyForm Noncommercial 1.0.0 plus the project's
-- AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
--
-- V2: Plan aggregate root with Household and Person (1..2) per ADR-002.
--
-- Plan owns Household (1..1) and Person (1..2). SalaryProfile is paired
-- 1..1 with each Person; its fields land in a later story (ADR-002 salary
-- timeline). FilingStatus is stored as TEXT with a CHECK constraint to
-- keep the migration database-portable while preserving the enum surface
-- in Java. All timestamps are TIMESTAMPTZ in UTC per CLAUDE.md.

CREATE TABLE plan (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_plan_tenant ON plan (tenant_id);

CREATE TABLE household (
    id              BIGSERIAL PRIMARY KEY,
    plan_id         BIGINT NOT NULL UNIQUE REFERENCES plan (id) ON DELETE CASCADE,
    filing_status   TEXT NOT NULL CHECK (filing_status IN (
        'SINGLE',
        'MARRIED_FILING_JOINTLY',
        'MARRIED_FILING_SEPARATELY',
        'HEAD_OF_HOUSEHOLD',
        'QUALIFYING_SURVIVING_SPOUSE'
    )),
    state           TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE salary_profile (
    id          BIGSERIAL PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE person (
    id                  BIGSERIAL PRIMARY KEY,
    plan_id             BIGINT NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    salary_profile_id   BIGINT NOT NULL UNIQUE REFERENCES salary_profile (id) ON DELETE CASCADE,
    dob                 DATE NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_person_plan ON person (plan_id);
