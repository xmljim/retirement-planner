-- Copyright (c) 2026 Jim Earley. All rights reserved.
-- Licensed under PolyForm Noncommercial 1.0.0 plus the project's
-- AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
--
-- V5: Plan-level Assumptions and per-Person retirement date for the
-- deterministic accumulation projector (S-2.8, FR-1.1, FR-5.1, ADR-002,
-- ADR-003).
--
-- Per-Person retirement date supports households where each spouse
-- retires on their own schedule. The projector horizon is the latest
-- date across the Plan's persons.
--
-- Plan-level Assumptions carry the deterministic-projection inputs
-- (pre-retirement return rate, cash interest rate). Stored as
-- NUMERIC(7,6) — six fractional digits matches Money's internal scale
-- and easily covers the precision domain (0.000000..0.999999).

ALTER TABLE plan
    ADD COLUMN pre_retirement_return_rate NUMERIC(7, 6) NOT NULL DEFAULT 0.07,
    ADD COLUMN cash_interest_rate         NUMERIC(7, 6) NOT NULL DEFAULT 0.04;

ALTER TABLE plan
    ALTER COLUMN pre_retirement_return_rate DROP DEFAULT,
    ALTER COLUMN cash_interest_rate DROP DEFAULT;

ALTER TABLE person
    ADD COLUMN retirement_date DATE NOT NULL DEFAULT DATE '2065-01-01';

ALTER TABLE person
    ALTER COLUMN retirement_date DROP DEFAULT;

ALTER TABLE person
    ADD CONSTRAINT person_retirement_after_dob CHECK (retirement_date > dob);
