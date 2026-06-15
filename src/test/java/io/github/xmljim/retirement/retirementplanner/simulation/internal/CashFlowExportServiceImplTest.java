/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;
import io.github.xmljim.retirement.retirementplanner.simulation.CashFlowExportService;
import io.github.xmljim.retirement.retirementplanner.simulation.MonthlyProjection;
import io.github.xmljim.retirement.retirementplanner.simulation.ProjectionPhase;

@DisplayName("CashFlowExportServiceImpl")
class CashFlowExportServiceImplTest {

    private static final YearMonth JUN_2026 = YearMonth.of(2026, 6);
    private static final YearMonth JUL_2026 = YearMonth.of(2026, 7);

    private final CashFlowExportService service = new CashFlowExportServiceImpl();

    @Test
    @DisplayName("emits header-only when no cash flows are present")
    void emptyLedgerRendersHeaderOnly() {
        String csv = service.toCsv(List.of(monthOnly(JUN_2026)));
        assertThat(csv).isEqualTo(CashFlowExportServiceImpl.HEADER);
    }

    @Test
    @DisplayName("renders one row per cash flow with display-scale-2 amount")
    void rendersRowPerCashFlow() {
        MonthlyProjection projection = monthWithFlows(
                JUN_2026,
                cashFlow(JUN_2026, 42L, CashFlowKind.EMPLOYEE_PRETAX, "1958.333333"),
                cashFlow(JUN_2026, 42L, CashFlowKind.EMPLOYER_MATCH, "1175.00"));

        String csv = service.toCsv(List.of(projection));

        assertThat(csv.lines().toList())
                .containsExactly(
                        CashFlowExportServiceImpl.HEADER,
                        "2026-06,42,EMPLOYEE_PRETAX,1958.33",
                        "2026-06,42,EMPLOYER_MATCH,1175.00");
    }

    @Test
    @DisplayName("preserves engine emission order across months")
    void preservesEmissionOrderAcrossMonths() {
        MonthlyProjection june = monthWithFlows(JUN_2026, cashFlow(JUN_2026, 1L, CashFlowKind.EMPLOYEE_HSA, "300.00"));
        MonthlyProjection july = monthWithFlows(JUL_2026, cashFlow(JUL_2026, 1L, CashFlowKind.EMPLOYEE_HSA, "300.00"));

        String csv = service.toCsv(List.of(june, july));

        assertThat(csv.lines().toList())
                .containsExactly(
                        CashFlowExportServiceImpl.HEADER,
                        "2026-06,1,EMPLOYEE_HSA,300.00",
                        "2026-07,1,EMPLOYEE_HSA,300.00");
    }

    @Test
    @DisplayName("uses CashFlowKind.name() exactly so reserved kinds round-trip")
    void reservedKindsRenderTheirEnumName() {
        MonthlyProjection projection = monthWithFlows(
                JUN_2026,
                cashFlow(JUN_2026, 7L, CashFlowKind.ROTH_CONVERSION_TAXABLE, "5000.00"),
                cashFlow(JUN_2026, 8L, CashFlowKind.WITHDRAWAL_ORDINARY, "2000.00"),
                cashFlow(JUN_2026, 9L, CashFlowKind.WITHDRAWAL_QUALIFIED, "1000.00"),
                cashFlow(JUN_2026, 10L, CashFlowKind.BUCKET_DRAW, "750.00"));

        String csv = service.toCsv(List.of(projection));

        assertThat(csv)
                .contains("ROTH_CONVERSION_TAXABLE")
                .contains("WITHDRAWAL_ORDINARY")
                .contains("WITHDRAWAL_QUALIFIED")
                .contains("BUCKET_DRAW");
    }

    private static MonthlyProjection monthOnly(YearMonth period) {
        return new MonthlyProjection(period, ProjectionPhase.ACCUMULATION, List.of(), List.of());
    }

    private static MonthlyProjection monthWithFlows(YearMonth period, CashFlow... flows) {
        return new MonthlyProjection(period, ProjectionPhase.ACCUMULATION, List.of(), List.of(flows));
    }

    private static CashFlow cashFlow(YearMonth period, long accountId, CashFlowKind kind, String amount) {
        return new CashFlow(period, accountId, kind, Money.usd(amount));
    }
}
