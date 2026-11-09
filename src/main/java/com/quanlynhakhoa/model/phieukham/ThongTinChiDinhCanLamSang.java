package com.quanlynhakhoa.model.phieukham;

import java.time.LocalDateTime;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class ThongTinChiDinhCanLamSang {
    private final String maChiDinh;
    private final String maPhieuKham;
    private final String maDichVu;
    private final String tenDichVu;
    private final String yeuCau;
    private final String ketQua;
    private final String maNhanVien;
    private final String tenBacSi;
    private final LocalDateTime thoiGianChiDinh;
    private final LocalDateTime thoiGianKetQua;
    private final String trangThai;

    public ThongTinChiDinhCanLamSang(String maChiDinh, String maPhieuKham, String maDichVu, String tenDichVu, String yeuCau, String ketQua, String maNhanVien, String tenBacSi, LocalDateTime thoiGianChiDinh, LocalDateTime thoiGianKetQua, String trangThai) {
        this.maChiDinh = maChiDinh;
        this.maPhieuKham = maPhieuKham;
        this.maDichVu = maDichVu;
        this.tenDichVu = tenDichVu;
        this.yeuCau = yeuCau;
        this.ketQua = ketQua;
        this.maNhanVien = maNhanVien;
        this.tenBacSi = tenBacSi;
        this.thoiGianChiDinh = thoiGianChiDinh;
        this.thoiGianKetQua = thoiGianKetQua;
        this.trangThai = trangThai;
    }

    public String maChiDinh() { return maChiDinh; }
    public String maPhieuKham() { return maPhieuKham; }
    public String maDichVu() { return maDichVu; }
    public String tenDichVu() { return tenDichVu; }
    public String yeuCau() { return yeuCau; }
    public String ketQua() { return ketQua; }
    public String maNhanVien() { return maNhanVien; }
    public String tenBacSi() { return tenBacSi; }
    public LocalDateTime thoiGianChiDinh() { return thoiGianChiDinh; }
    public LocalDateTime thoiGianKetQua() { return thoiGianKetQua; }
    public String trangThai() { return trangThai; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinChiDinhCanLamSang value)) return false;
        return java.util.Objects.equals(maChiDinh, value.maChiDinh)
                && java.util.Objects.equals(maPhieuKham, value.maPhieuKham)
                && java.util.Objects.equals(maDichVu, value.maDichVu)
                && java.util.Objects.equals(tenDichVu, value.tenDichVu)
                && java.util.Objects.equals(yeuCau, value.yeuCau)
                && java.util.Objects.equals(ketQua, value.ketQua)
                && java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(tenBacSi, value.tenBacSi)
                && java.util.Objects.equals(thoiGianChiDinh, value.thoiGianChiDinh)
                && java.util.Objects.equals(thoiGianKetQua, value.thoiGianKetQua)
                && java.util.Objects.equals(trangThai, value.trangThai);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maChiDinh);
        result = 31 * result + java.util.Objects.hashCode(maPhieuKham);
        result = 31 * result + java.util.Objects.hashCode(maDichVu);
        result = 31 * result + java.util.Objects.hashCode(tenDichVu);
        result = 31 * result + java.util.Objects.hashCode(yeuCau);
        result = 31 * result + java.util.Objects.hashCode(ketQua);
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(tenBacSi);
        result = 31 * result + java.util.Objects.hashCode(thoiGianChiDinh);
        result = 31 * result + java.util.Objects.hashCode(thoiGianKetQua);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        return result;
    }

    @Override
    public String toString() {
        return "ChiDinhCanLamSang[" + "maChiDinh=" + maChiDinh + ", " + "maPhieuKham=" + maPhieuKham + ", " + "maDichVu=" + maDichVu + ", " + "tenDichVu=" + tenDichVu + ", " + "yeuCau=" + yeuCau + ", " + "ketQua=" + ketQua + ", " + "maNhanVien=" + maNhanVien + ", " + "tenBacSi=" + tenBacSi + ", " + "thoiGianChiDinh=" + thoiGianChiDinh + ", " + "thoiGianKetQua=" + thoiGianKetQua + ", " + "trangThai=" + trangThai + "]";
    }
}
