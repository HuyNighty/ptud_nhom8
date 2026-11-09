package com.quanlynhakhoa.model.phieukham;

import java.time.LocalDateTime;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class ThongTinPhieuKham {
    private final String maPhieuKham;
    private final String maBenhNhan;
    private final String tenBenhNhan;
    private final String soDienThoai;
    private final String maHoSo;
    private final String maLichHen;
    private final LocalDateTime thoiGianTiepNhan;
    private final String maNhanVien;
    private final String tenBacSi;
    private final String trieuChung;
    private final String chanDoan;
    private final String ketLuan;
    private final String trangThai;
    private final String tienSuBenh;
    private final String diUng;

    public ThongTinPhieuKham(String maPhieuKham, String maBenhNhan, String tenBenhNhan, String soDienThoai, String maHoSo, String maLichHen, LocalDateTime thoiGianTiepNhan, String maNhanVien, String tenBacSi, String trieuChung, String chanDoan, String ketLuan, String trangThai, String tienSuBenh, String diUng) {
        this.maPhieuKham = maPhieuKham;
        this.maBenhNhan = maBenhNhan;
        this.tenBenhNhan = tenBenhNhan;
        this.soDienThoai = soDienThoai;
        this.maHoSo = maHoSo;
        this.maLichHen = maLichHen;
        this.thoiGianTiepNhan = thoiGianTiepNhan;
        this.maNhanVien = maNhanVien;
        this.tenBacSi = tenBacSi;
        this.trieuChung = trieuChung;
        this.chanDoan = chanDoan;
        this.ketLuan = ketLuan;
        this.trangThai = trangThai;
        this.tienSuBenh = tienSuBenh;
        this.diUng = diUng;
    }

    public String maPhieuKham() { return maPhieuKham; }
    public String maBenhNhan() { return maBenhNhan; }
    public String tenBenhNhan() { return tenBenhNhan; }
    public String soDienThoai() { return soDienThoai; }
    public String maHoSo() { return maHoSo; }
    public String maLichHen() { return maLichHen; }
    public LocalDateTime thoiGianTiepNhan() { return thoiGianTiepNhan; }
    public String maNhanVien() { return maNhanVien; }
    public String tenBacSi() { return tenBacSi; }
    public String trieuChung() { return trieuChung; }
    public String chanDoan() { return chanDoan; }
    public String ketLuan() { return ketLuan; }
    public String trangThai() { return trangThai; }
    public String tienSuBenh() { return tienSuBenh; }
    public String diUng() { return diUng; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinPhieuKham value)) return false;
        return java.util.Objects.equals(maPhieuKham, value.maPhieuKham)
                && java.util.Objects.equals(maBenhNhan, value.maBenhNhan)
                && java.util.Objects.equals(tenBenhNhan, value.tenBenhNhan)
                && java.util.Objects.equals(soDienThoai, value.soDienThoai)
                && java.util.Objects.equals(maHoSo, value.maHoSo)
                && java.util.Objects.equals(maLichHen, value.maLichHen)
                && java.util.Objects.equals(thoiGianTiepNhan, value.thoiGianTiepNhan)
                && java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(tenBacSi, value.tenBacSi)
                && java.util.Objects.equals(trieuChung, value.trieuChung)
                && java.util.Objects.equals(chanDoan, value.chanDoan)
                && java.util.Objects.equals(ketLuan, value.ketLuan)
                && java.util.Objects.equals(trangThai, value.trangThai)
                && java.util.Objects.equals(tienSuBenh, value.tienSuBenh)
                && java.util.Objects.equals(diUng, value.diUng);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maPhieuKham);
        result = 31 * result + java.util.Objects.hashCode(maBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(tenBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(soDienThoai);
        result = 31 * result + java.util.Objects.hashCode(maHoSo);
        result = 31 * result + java.util.Objects.hashCode(maLichHen);
        result = 31 * result + java.util.Objects.hashCode(thoiGianTiepNhan);
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(tenBacSi);
        result = 31 * result + java.util.Objects.hashCode(trieuChung);
        result = 31 * result + java.util.Objects.hashCode(chanDoan);
        result = 31 * result + java.util.Objects.hashCode(ketLuan);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        result = 31 * result + java.util.Objects.hashCode(tienSuBenh);
        result = 31 * result + java.util.Objects.hashCode(diUng);
        return result;
    }

    @Override
    public String toString() {
        return "PhieuKham[" + "maPhieuKham=" + maPhieuKham + ", " + "maBenhNhan=" + maBenhNhan + ", " + "tenBenhNhan=" + tenBenhNhan + ", " + "soDienThoai=" + soDienThoai + ", " + "maHoSo=" + maHoSo + ", " + "maLichHen=" + maLichHen + ", " + "thoiGianTiepNhan=" + thoiGianTiepNhan + ", " + "maNhanVien=" + maNhanVien + ", " + "tenBacSi=" + tenBacSi + ", " + "trieuChung=" + trieuChung + ", " + "chanDoan=" + chanDoan + ", " + "ketLuan=" + ketLuan + ", " + "trangThai=" + trangThai + ", " + "tienSuBenh=" + tienSuBenh + ", " + "diUng=" + diUng + "]";
    }
}
