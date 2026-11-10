package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class PhieuKham {
    private final String maPhieuKham;
    private final String maBenhNhan;
    private final String maHoSo;
    private final String maLichHen;
    private final LocalDateTime thoiGianTiepNhan;
    private final String maNhanVien;
    private final String trieuChung;
    private final String chanDoan;
    private final String ketLuan;
    private final String trangThai;

    public PhieuKham(String maPhieuKham, String maBenhNhan, String maHoSo, String maLichHen, LocalDateTime thoiGianTiepNhan, String maNhanVien, String trieuChung, String chanDoan, String ketLuan, String trangThai) {
        this.maPhieuKham = maPhieuKham;
        this.maBenhNhan = maBenhNhan;
        this.maHoSo = maHoSo;
        this.maLichHen = maLichHen;
        this.thoiGianTiepNhan = thoiGianTiepNhan;
        this.maNhanVien = maNhanVien;
        this.trieuChung = trieuChung;
        this.chanDoan = chanDoan;
        this.ketLuan = ketLuan;
        this.trangThai = trangThai;
    }

    public String getMaPhieuKham() { return maPhieuKham; }
    public String getMaBenhNhan() { return maBenhNhan; }
    public String getMaHoSo() { return maHoSo; }
    public String getMaLichHen() { return maLichHen; }
    public LocalDateTime getThoiGianTiepNhan() { return thoiGianTiepNhan; }
    public String getMaNhanVien() { return maNhanVien; }
    public String getTrieuChung() { return trieuChung; }
    public String getChanDoan() { return chanDoan; }
    public String getKetLuan() { return ketLuan; }
    public String getTrangThai() { return trangThai; }
}
