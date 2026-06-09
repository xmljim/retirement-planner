-- Copyright (c) 2026 Jim Earley. All rights reserved.
-- Licensed under PolyForm Noncommercial 1.0.0 plus the project's
-- AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
--
-- V1 baseline: tenant scoping table.
--
-- ADR-001 frames the SaaS phase as multi-tenant; this baseline introduces
-- the table that downstream tables will reference, and seeds the single
-- 'solo' tenant used by v1 (solo-user) deployments. The auth stub injects
-- this tenant for all requests until the passkey provider lands
-- (S-1.8 / EPIC-8).

CREATE TABLE tenants (
    id           BIGSERIAL PRIMARY KEY,
    slug         TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO tenants (slug, display_name) VALUES ('solo', 'Solo');
