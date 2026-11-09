package com.quanlynhakhoa.model.phieukham;

import com.quanlynhakhoa.model.phieukham.ThuocChonKeDon;
import java.util.List;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DuLieuDonThuoc {
    private final List<ThuocChonKeDon> danhSachThuoc;
    private final List<DongChiTietDonThuoc> chiTiet;
    private final String loiDan;
    private final boolean daKeDon;

    public DuLieuDonThuoc(List<ThuocChonKeDon> danhSachThuoc, List<DongChiTietDonThuoc> chiTiet, String loiDan, boolean daKeDon) {
        danhSachThuoc = List.copyOf(danhSachThuoc);
        chiTiet = List.copyOf(chiTiet);
        loiDan = loiDan == null ? "" : loiDan;
        this.danhSachThuoc = danhSachThuoc;
        this.chiTiet = chiTiet;
        this.loiDan = loiDan;
        this.daKeDon = daKeDon;
    }

    public List<ThuocChonKeDon> danhSachThuoc() { return danhSachThuoc; }
    public List<DongChiTietDonThuoc> chiTiet() { return chiTiet; }
    public String loiDan() { return loiDan; }
    public boolean daKeDon() { return daKeDon; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuLieuDonThuoc value)) return false;
        return java.util.Objects.equals(danhSachThuoc, value.danhSachThuoc)
                && java.util.Objects.equals(chiTiet, value.chiTiet)
                && java.util.Objects.equals(loiDan, value.loiDan)
                && daKeDon == value.daKeDon;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(danhSachThuoc);
        result = 31 * result + java.util.Objects.hashCode(chiTiet);
        result = 31 * result + java.util.Objects.hashCode(loiDan);
        result = 31 * result + Boolean.hashCode(daKeDon);
        return result;
    }

    @Override
    public String toString() {
        return "DonThuocData[" + "danhSachThuoc=" + danhSachThuoc + ", " + "chiTiet=" + chiTiet + ", " + "loiDan=" + loiDan + ", " + "daKeDon=" + daKeDon + "]";
    }
}
