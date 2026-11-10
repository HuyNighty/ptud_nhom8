package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class HoSoBenhAn {
    private String maHoSo;
    private String maBenhNhan;
    private LocalDate ngayLap;
    private String tienSuBenh;
    private String diUng;

    public HoSoBenhAn() { this(null, null, null, null, null); }
    public HoSoBenhAn(String maHoSo) { this(maHoSo, null, null, null, null); }

    public HoSoBenhAn(String maHoSo, String maBenhNhan, LocalDate ngayLap, String tienSuBenh, String diUng) {
        this.maHoSo = maHoSo;
        this.maBenhNhan = maBenhNhan;
        this.ngayLap = ngayLap;
        this.tienSuBenh = tienSuBenh;
        this.diUng = diUng;
    }

    public String getMaHoSo() { return maHoSo; }
    public void setMaHoSo(String maHoSo) { this.maHoSo = maHoSo; }
    public String getMaBenhNhan() { return maBenhNhan; }
    public void setMaBenhNhan(String maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    public LocalDate getNgayLap() { return ngayLap; }
    public void setNgayLap(LocalDate ngayLap) { this.ngayLap = ngayLap; }
    public String getTienSuBenh() { return tienSuBenh; }
    public void setTienSuBenh(String tienSuBenh) { this.tienSuBenh = tienSuBenh; }
    public String getDiUng() { return diUng; }
    public void setDiUng(String diUng) { this.diUng = diUng; }
}
