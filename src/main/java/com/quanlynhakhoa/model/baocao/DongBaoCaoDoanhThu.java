package com.quanlynhakhoa.model.baocao;

import java.math.BigDecimal;
import java.time.LocalDate;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongBaoCaoDoanhThu {
    private final LocalDate period;
    private final int invoices;
    private final BigDecimal revenue;
    private final BigDecimal collected;

    public DongBaoCaoDoanhThu(LocalDate period, int invoices, BigDecimal revenue, BigDecimal collected) {
        this.period = period;
        this.invoices = invoices;
        this.revenue = revenue;
        this.collected = collected;
    }

    public LocalDate period() { return period; }
    public int invoices() { return invoices; }
    public BigDecimal revenue() { return revenue; }
    public BigDecimal collected() { return collected; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongBaoCaoDoanhThu value)) return false;
        return java.util.Objects.equals(period, value.period)
                && invoices == value.invoices
                && java.util.Objects.equals(revenue, value.revenue)
                && java.util.Objects.equals(collected, value.collected);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.period);
        result = 31 * result + Integer.hashCode(this.invoices);
        result = 31 * result + java.util.Objects.hashCode(this.revenue);
        result = 31 * result + java.util.Objects.hashCode(this.collected);
        return result;
    }

    @Override
    public String toString() {
        return "Row[" + "period=" + period + ", " + "invoices=" + invoices + ", " + "revenue=" + revenue + ", " + "collected=" + collected + "]";
    }
}
