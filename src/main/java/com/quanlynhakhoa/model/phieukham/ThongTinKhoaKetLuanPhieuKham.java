package com.quanlynhakhoa.model.phieukham;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class ThongTinKhoaKetLuanPhieuKham {
    private final String maBenhNhan;
    private final String maNhanVien;
    private final String maLichHen;
    private final String trangThai;

    public ThongTinKhoaKetLuanPhieuKham(String maBenhNhan, String maNhanVien, String maLichHen, String trangThai) {
        this.maBenhNhan = maBenhNhan;
        this.maNhanVien = maNhanVien;
        this.maLichHen = maLichHen;
        this.trangThai = trangThai;
    }

    public String maBenhNhan() { return maBenhNhan; }
    public String maNhanVien() { return maNhanVien; }
    public String maLichHen() { return maLichHen; }
    public String trangThai() { return trangThai; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinKhoaKetLuanPhieuKham value)) return false;
        return java.util.Objects.equals(maBenhNhan, value.maBenhNhan)
                && java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(maLichHen, value.maLichHen)
                && java.util.Objects.equals(trangThai, value.trangThai);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(maLichHen);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        return result;
    }

    @Override
    public String toString() {
        return "PhieuKhamKetLuanLock[" + "maBenhNhan=" + maBenhNhan + ", " + "maNhanVien=" + maNhanVien + ", " + "maLichHen=" + maLichHen + ", " + "trangThai=" + trangThai + "]";
    }
}
