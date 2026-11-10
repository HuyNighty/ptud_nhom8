package com.quanlynhakhoa.entity;

import java.time.LocalDate;

/** Dữ liệu nghiệp vụ theo schema hiện có; Service/DAO điều phối lưu và chuyển trạng thái. */
public final class KeHoachDieuTri {
    private final String maKeHoach;
    private final String maHoSo;
    private final String maPhieuKham;
    private final LocalDate ngayLap;
    private String mucTieu;
    private final String trangThai;
    private final String maNhanVien;
    private final String lyDoHuy;

    public KeHoachDieuTri(String maKeHoach, String maHoSo, String maPhieuKham, LocalDate ngayLap, String mucTieu, String trangThai, String maNhanVien, String lyDoHuy) {
        this.maKeHoach = maKeHoach;
        this.maHoSo = maHoSo;
        this.maPhieuKham = maPhieuKham;
        this.ngayLap = ngayLap;
        this.mucTieu = mucTieu;
        this.trangThai = trangThai;
        this.maNhanVien = maNhanVien;
        this.lyDoHuy = lyDoHuy;
    }

    public String getMaKeHoach() { return maKeHoach; }
    public String getMaHoSo() { return maHoSo; }
    public String getMaPhieuKham() { return maPhieuKham; }
    public LocalDate getNgayLap() { return ngayLap; }
    public String getMucTieu() { return mucTieu; }
    public void setMucTieu(String mucTieu) { this.mucTieu = mucTieu; }
    public String getTrangThai() { return trangThai; }
    public String getMaNhanVien() { return maNhanVien; }
    public String getLyDoHuy() { return lyDoHuy; }
}
