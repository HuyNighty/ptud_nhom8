package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;
import java.util.List;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class BanNhapHoaDon {
    private final String patientId;
    private final String visitId;
    private final String revision;
    private final List<DongBanNhapHoaDon> lines;

    public BanNhapHoaDon(String patientId, String visitId, String revision, List<DongBanNhapHoaDon> lines) {
        lines = List.copyOf(lines);
        this.patientId = patientId;
        this.visitId = visitId;
        this.revision = revision;
        this.lines = lines;
    }

    public String patientId() { return patientId; }
    public String visitId() { return visitId; }
    public String revision() { return revision; }
    public List<DongBanNhapHoaDon> lines() { return lines; }



    public BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO;
        for (DongBanNhapHoaDon line : lines) total = total.add(line.amount());
        return total;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BanNhapHoaDon value)) return false;
        return java.util.Objects.equals(patientId, value.patientId)
                && java.util.Objects.equals(visitId, value.visitId)
                && java.util.Objects.equals(revision, value.revision)
                && java.util.Objects.equals(lines, value.lines);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(patientId);
        result = 31 * result + java.util.Objects.hashCode(visitId);
        result = 31 * result + java.util.Objects.hashCode(revision);
        result = 31 * result + java.util.Objects.hashCode(lines);
        return result;
    }

    @Override
    public String toString() {
        return "InvoiceDraft[" + "patientId=" + patientId + ", " + "visitId=" + visitId + ", " + "revision=" + revision + ", " + "lines=" + lines + "]";
    }
}
