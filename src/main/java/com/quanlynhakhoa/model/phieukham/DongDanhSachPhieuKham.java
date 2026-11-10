package com.quanlynhakhoa.model.phieukham;

import java.time.LocalDateTime;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DongDanhSachPhieuKham {
    private final String maPhieuKham;
    private final String maBenhNhan;
    private final String tenBenhNhan;
    private final String soDienThoai;
    private final LocalDateTime thoiGianTiepNhan;
    private final String maNhanVien;
    private final String tenBacSi;
    private final String trangThai;

    public DongDanhSachPhieuKham(String maPhieuKham, String maBenhNhan, String tenBenhNhan, String soDienThoai, LocalDateTime thoiGianTiepNhan, String maNhanVien, String tenBacSi, String trangThai) {
        this.maPhieuKham = maPhieuKham;
        this.maBenhNhan = maBenhNhan;
        this.tenBenhNhan = tenBenhNhan;
        this.soDienThoai = soDienThoai;
        this.thoiGianTiepNhan = thoiGianTiepNhan;
        this.maNhanVien = maNhanVien;
        this.tenBacSi = tenBacSi;
        this.trangThai = trangThai;
    }

    public String maPhieuKham() { return maPhieuKham; }
    public String maBenhNhan() { return maBenhNhan; }
    public String tenBenhNhan() { return tenBenhNhan; }
    public String soDienThoai() { return soDienThoai; }
    public LocalDateTime thoiGianTiepNhan() { return thoiGianTiepNhan; }
    public String maNhanVien() { return maNhanVien; }
    public String tenBacSi() { return tenBacSi; }
    public String trangThai() { return trangThai; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongDanhSachPhieuKham value)) return false;
        return java.util.Objects.equals(maPhieuKham, value.maPhieuKham)
                && java.util.Objects.equals(maBenhNhan, value.maBenhNhan)
                && java.util.Objects.equals(tenBenhNhan, value.tenBenhNhan)
                && java.util.Objects.equals(soDienThoai, value.soDienThoai)
                && java.util.Objects.equals(thoiGianTiepNhan, value.thoiGianTiepNhan)
                && java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(tenBacSi, value.tenBacSi)
                && java.util.Objects.equals(trangThai, value.trangThai);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maPhieuKham);
        result = 31 * result + java.util.Objects.hashCode(maBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(tenBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(soDienThoai);
        result = 31 * result + java.util.Objects.hashCode(thoiGianTiepNhan);
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(tenBacSi);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        return result;
    }

    @Override
    public String toString() {
        return "PhieuKhamListItem[" + "maPhieuKham=" + maPhieuKham + ", " + "maBenhNhan=" + maBenhNhan + ", " + "tenBenhNhan=" + tenBenhNhan + ", " + "soDienThoai=" + soDienThoai + ", " + "thoiGianTiepNhan=" + thoiGianTiepNhan + ", " + "maNhanVien=" + maNhanVien + ", " + "tenBacSi=" + tenBacSi + ", " + "trangThai=" + trangThai + "]";
    }
}
