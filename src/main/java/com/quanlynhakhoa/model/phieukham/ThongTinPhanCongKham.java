package com.quanlynhakhoa.model.phieukham;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class ThongTinPhanCongKham {
    private final String maNhanVien;
    private final String trangThai;

    public ThongTinPhanCongKham(String maNhanVien, String trangThai) {
        this.maNhanVien = maNhanVien;
        this.trangThai = trangThai;
    }

    public String maNhanVien() { return maNhanVien; }
    public String trangThai() { return trangThai; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinPhanCongKham value)) return false;
        return java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(trangThai, value.trangThai);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        return result;
    }

    @Override
    public String toString() {
        return "PhieuKhamAssignment[" + "maNhanVien=" + maNhanVien + ", " + "trangThai=" + trangThai + "]";
    }
}
