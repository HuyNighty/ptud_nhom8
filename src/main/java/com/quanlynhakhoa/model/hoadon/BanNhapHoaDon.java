package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;
import java.util.List;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class BanNhapHoaDon {
    private final String maBenhNhan;
    private final String maPhieuKham;
    private final String phienBan;
    private final List<DongBanNhapHoaDon> chiTiet;

    public BanNhapHoaDon(String maBenhNhan, String maPhieuKham, String phienBan, List<DongBanNhapHoaDon> chiTiet) {
        chiTiet = List.copyOf(chiTiet);
        this.maBenhNhan = maBenhNhan;
        this.maPhieuKham = maPhieuKham;
        this.phienBan = phienBan;
        this.chiTiet = chiTiet;
    }

    public String patientId() { return maBenhNhan; }
    public String visitId() { return maPhieuKham; }
    public String revision() { return phienBan; }
    public List<DongBanNhapHoaDon> lines() { return chiTiet; }



    public BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO;
        for (DongBanNhapHoaDon line : chiTiet) total = total.add(line.amount());
        return total;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BanNhapHoaDon value)) return false;
        return java.util.Objects.equals(maBenhNhan, value.maBenhNhan)
                && java.util.Objects.equals(maPhieuKham, value.maPhieuKham)
                && java.util.Objects.equals(phienBan, value.phienBan)
                && java.util.Objects.equals(chiTiet, value.chiTiet);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(maPhieuKham);
        result = 31 * result + java.util.Objects.hashCode(phienBan);
        result = 31 * result + java.util.Objects.hashCode(chiTiet);
        return result;
    }

    @Override
    public String toString() {
        return "InvoiceDraft[" + "patientId=" + maBenhNhan + ", " + "visitId=" + maPhieuKham + ", " + "revision=" + phienBan + ", " + "lines=" + chiTiet + "]";
    }
}
