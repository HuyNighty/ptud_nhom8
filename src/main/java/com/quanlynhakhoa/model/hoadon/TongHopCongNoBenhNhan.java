package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import java.math.BigDecimal;
import java.util.List;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class TongHopCongNoBenhNhan {
    private final String patientId;
    private final String name;
    private final BigDecimal total;
    private final BigDecimal collected;
    private final BigDecimal debt;
    private final List<ThongTinHoaDon> invoices;

    public TongHopCongNoBenhNhan(String patientId, String name, BigDecimal total, BigDecimal collected, BigDecimal debt, List<ThongTinHoaDon> invoices) {
        invoices=List.copyOf(invoices);
        this.patientId = patientId;
        this.name = name;
        this.total = total;
        this.collected = collected;
        this.debt = debt;
        this.invoices = invoices;
    }

    public String patientId() { return patientId; }
    public String name() { return name; }
    public BigDecimal total() { return total; }
    public BigDecimal collected() { return collected; }
    public BigDecimal debt() { return debt; }
    public List<ThongTinHoaDon> invoices() { return invoices; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TongHopCongNoBenhNhan value)) return false;
        return java.util.Objects.equals(patientId, value.patientId)
                && java.util.Objects.equals(name, value.name)
                && java.util.Objects.equals(total, value.total)
                && java.util.Objects.equals(collected, value.collected)
                && java.util.Objects.equals(debt, value.debt)
                && java.util.Objects.equals(invoices, value.invoices);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(patientId);
        result = 31 * result + java.util.Objects.hashCode(name);
        result = 31 * result + java.util.Objects.hashCode(total);
        result = 31 * result + java.util.Objects.hashCode(collected);
        result = 31 * result + java.util.Objects.hashCode(debt);
        result = 31 * result + java.util.Objects.hashCode(invoices);
        return result;
    }

    @Override
    public String toString() {
        return "PatientDebt[" + "patientId=" + patientId + ", " + "name=" + name + ", " + "total=" + total + ", " + "collected=" + collected + ", " + "debt=" + debt + ", " + "invoices=" + invoices + "]";
    }
}
