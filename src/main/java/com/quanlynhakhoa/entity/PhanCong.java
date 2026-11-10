package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class PhanCong {
    private final String maPhanCong;
    private final String maNhanVien;
    private final CaLamViec ca;
    private final LocalDate ngayLam;
    private final String nhiemVu;
    private final String trangThai;
    private final String lyDoHuy;

    public PhanCong(String maPhanCong, String maNhanVien, CaLamViec ca, LocalDate ngayLam, String nhiemVu, String trangThai, String lyDoHuy) {
        this.maPhanCong = maPhanCong;
        this.maNhanVien = maNhanVien;
        this.ca = ca;
        this.ngayLam = ngayLam;
        this.nhiemVu = nhiemVu;
        this.trangThai = trangThai;
        this.lyDoHuy = lyDoHuy;
    }

    public String getMaPhanCong() { return maPhanCong; }
    public String getMaNhanVien() { return maNhanVien; }
    public CaLamViec getCa() { return ca; }
    public LocalDate getNgayLam() { return ngayLam; }
    public String getNhiemVu() { return nhiemVu; }
    public String getTrangThai() { return trangThai; }
    public String getLyDoHuy() { return lyDoHuy; }
}
