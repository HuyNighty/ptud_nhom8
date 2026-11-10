package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class HoaDon {
    private final String maHoaDon;
    private final String maNhanVien;
    private final String maBenhNhan;
    private final String maPhieuKham;
    private final LocalDateTime ngayLap;
    private final TrangThaiHoaDon trangThai;

    public HoaDon(String maHoaDon, String maNhanVien, String maBenhNhan, String maPhieuKham, LocalDateTime ngayLap, TrangThaiHoaDon trangThai) {
        this.maHoaDon = maHoaDon;
        this.maNhanVien = maNhanVien;
        this.maBenhNhan = maBenhNhan;
        this.maPhieuKham = maPhieuKham;
        this.ngayLap = ngayLap;
        this.trangThai = trangThai;
    }

    public String getMaHoaDon() { return maHoaDon; }
    public String getMaNhanVien() { return maNhanVien; }
    public String getMaBenhNhan() { return maBenhNhan; }
    public String getMaPhieuKham() { return maPhieuKham; }
    public LocalDateTime getNgayLap() { return ngayLap; }
    public TrangThaiHoaDon getTrangThai() { return trangThai; }
}
