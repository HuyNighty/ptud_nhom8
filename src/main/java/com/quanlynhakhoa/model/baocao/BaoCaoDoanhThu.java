package com.quanlynhakhoa.model.baocao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class BaoCaoDoanhThu {
    private final LocalDate from;
    private final LocalDate to;
    private final boolean monthly;
    private final List<DongBaoCaoDoanhThu> rows;
    private final BigDecimal revenue;
    private final BigDecimal collected;
    private final BigDecimal closingDebt;
    private final int invoices;

    public BaoCaoDoanhThu(LocalDate from, LocalDate to, boolean monthly, List<DongBaoCaoDoanhThu> rows, BigDecimal revenue, BigDecimal collected, BigDecimal closingDebt, int invoices) {
        rows = List.copyOf(rows);
        this.from = from;
        this.to = to;
        this.monthly = monthly;
        this.rows = rows;
        this.revenue = revenue;
        this.collected = collected;
        this.closingDebt = closingDebt;
        this.invoices = invoices;
    }

    public LocalDate from() { return from; }
    public LocalDate to() { return to; }
    public boolean monthly() { return monthly; }
    public List<DongBaoCaoDoanhThu> rows() { return rows; }
    public BigDecimal revenue() { return revenue; }
    public BigDecimal collected() { return collected; }
    public BigDecimal closingDebt() { return closingDebt; }
    public int invoices() { return invoices; }




    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BaoCaoDoanhThu value)) return false;
        return java.util.Objects.equals(from, value.from)
                && java.util.Objects.equals(to, value.to)
                && monthly == value.monthly
                && java.util.Objects.equals(rows, value.rows)
                && java.util.Objects.equals(revenue, value.revenue)
                && java.util.Objects.equals(collected, value.collected)
                && java.util.Objects.equals(closingDebt, value.closingDebt)
                && invoices == value.invoices;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.from);
        result = 31 * result + java.util.Objects.hashCode(this.to);
        result = 31 * result + Boolean.hashCode(this.monthly);
        result = 31 * result + java.util.Objects.hashCode(this.rows);
        result = 31 * result + java.util.Objects.hashCode(this.revenue);
        result = 31 * result + java.util.Objects.hashCode(this.collected);
        result = 31 * result + java.util.Objects.hashCode(this.closingDebt);
        result = 31 * result + Integer.hashCode(this.invoices);
        return result;
    }

    @Override
    public String toString() {
        return "RevenueReport[" + "from=" + from + ", " + "to=" + to + ", " + "monthly=" + monthly + ", " + "rows=" + rows + ", " + "revenue=" + revenue + ", " + "collected=" + collected + ", " + "closingDebt=" + closingDebt + ", " + "invoices=" + invoices + "]";
    }
}
