/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.MoneyDisplay;
import io.github.xmljim.retirement.retirementplanner.simulation.CashFlowExportService;
import io.github.xmljim.retirement.retirementplanner.simulation.MonthlyProjection;

/**
 * RFC 4180 CSV rendering of {@link MonthlyProjection#cashFlows()}.
 *
 * <p>Column set ({@code period, accountId, kind, amount}) intentionally
 * mirrors the shape that the EPIC-6 Parquet snapshot writer will use,
 * so the audit export and future persisted form share one tuple
 * definition.
 */
@Service
class CashFlowExportServiceImpl implements CashFlowExportService {

    static final String HEADER = "period,accountId,kind,amount";

    @Override
    public String toCsv(List<MonthlyProjection> projections) {
        StringBuilder out = new StringBuilder(HEADER).append('\n');
        projections.stream().flatMap(p -> p.cashFlows().stream()).forEach(flow -> appendRow(out, flow));
        // Strip the trailing LF added by the last appendRow so the body
        // is header-only when the ledger is empty and otherwise has no
        // blank final line.
        int last = out.length() - 1;
        if (last >= 0 && out.charAt(last) == '\n') {
            out.deleteCharAt(last);
        }
        return out.toString();
    }

    private static void appendRow(StringBuilder out, CashFlow flow) {
        out.append(flow.period())
                .append(',')
                .append(flow.accountId())
                .append(',')
                .append(flow.kind().name())
                .append(',')
                .append(MoneyDisplay.toDisplay(flow.amount()).toPlainString())
                .append('\n');
    }
}
