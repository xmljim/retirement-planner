/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation;

import java.util.List;

/**
 * Audit-grade serialization of a deterministic projection's cash-flow
 * ledger (S-2.9, NFR-7).
 *
 * <p>Lives in {@code simulation/} alongside {@link ProjectionService}
 * because the export is a capability of the projection itself — same
 * tuple shape (period, accountId, kind, amount) that the EPIC-6
 * Parquet snapshot writer will use. The {@code api/} module stays pure
 * HTTP plumbing: it receives the request, calls this service, and
 * hands back the response.
 *
 * <p>Implementations are stateless. Future formats (Parquet, JSON
 * lines) can land as additional methods on this interface or as
 * sibling services without touching the controller.
 */
public interface CashFlowExportService {

    /**
     * Renders one row per cash flow across the given projections,
     * preserving the engine's emission order.
     *
     * @param projections the per-month projections to flatten
     * @return CSV body with header row + one row per cash flow,
     *         LF-terminated lines (no trailing newline). Empty
     *         ledger renders header-only.
     */
    String toCsv(List<MonthlyProjection> projections);
}
