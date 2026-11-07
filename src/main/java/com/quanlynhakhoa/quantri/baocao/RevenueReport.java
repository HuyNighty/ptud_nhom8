package com.quanlynhakhoa.quantri.baocao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueReport(LocalDate from, LocalDate to, boolean monthly, List<Row> rows,
                            BigDecimal revenue, BigDecimal collected, BigDecimal closingDebt, int invoices) {
    public RevenueReport { rows = List.copyOf(rows); }
    public record Row(LocalDate period, int invoices, BigDecimal revenue, BigDecimal collected) { }
}
