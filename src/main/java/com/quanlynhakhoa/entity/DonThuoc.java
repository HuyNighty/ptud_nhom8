package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class DonThuoc {
    private final String maDonThuoc;
    private final String maPhieuKham;
    private final String maBenhNhan;
    private final String maNhanVien;
    private final LocalDate ngayKe;
    private String loiDan;

    public DonThuoc(String maDonThuoc, String maPhieuKham, String maBenhNhan, String maNhanVien, LocalDate ngayKe, String loiDan) {
        this.maDonThuoc = maDonThuoc;
        this.maPhieuKham = maPhieuKham;
        this.maBenhNhan = maBenhNhan;
        this.maNhanVien = maNhanVien;
        this.ngayKe = ngayKe;
        this.loiDan = loiDan;
    }

    public String getMaDonThuoc() { return maDonThuoc; }
    public String getMaPhieuKham() { return maPhieuKham; }
    public String getMaBenhNhan() { return maBenhNhan; }
    public String getMaNhanVien() { return maNhanVien; }
    public LocalDate getNgayKe() { return ngayKe; }
    public String getLoiDan() { return loiDan; }
    public void setLoiDan(String loiDan) { this.loiDan = loiDan; }
}
