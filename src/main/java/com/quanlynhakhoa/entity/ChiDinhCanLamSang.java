package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class ChiDinhCanLamSang {
    private final String maChiDinh;
    private final String maPhieuKham;
    private final String maDichVu;
    private final String yeuCau;
    private final String ketQua;
    private final String maNhanVien;
    private final LocalDateTime thoiGianChiDinh;
    private final LocalDateTime thoiGianKetQua;
    private final String trangThai;

    public ChiDinhCanLamSang(String maChiDinh, String maPhieuKham, String maDichVu, String yeuCau, String ketQua, String maNhanVien, LocalDateTime thoiGianChiDinh, LocalDateTime thoiGianKetQua, String trangThai) {
        this.maChiDinh = maChiDinh;
        this.maPhieuKham = maPhieuKham;
        this.maDichVu = maDichVu;
        this.yeuCau = yeuCau;
        this.ketQua = ketQua;
        this.maNhanVien = maNhanVien;
        this.thoiGianChiDinh = thoiGianChiDinh;
        this.thoiGianKetQua = thoiGianKetQua;
        this.trangThai = trangThai;
    }

    public String getMaChiDinh() { return maChiDinh; }
    public String getMaPhieuKham() { return maPhieuKham; }
    public String getMaDichVu() { return maDichVu; }
    public String getYeuCau() { return yeuCau; }
    public String getKetQua() { return ketQua; }
    public String getMaNhanVien() { return maNhanVien; }
    public LocalDateTime getThoiGianChiDinh() { return thoiGianChiDinh; }
    public LocalDateTime getThoiGianKetQua() { return thoiGianKetQua; }
    public String getTrangThai() { return trangThai; }
}
