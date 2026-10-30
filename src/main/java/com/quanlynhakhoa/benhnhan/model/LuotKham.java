package com.quanlynhakhoa.benhnhan.model;

import java.time.LocalDateTime;

public class LuotKham {
    private String maPhieuKham;
    private LocalDateTime thoiGianTiepNhan;
    private String tenBacSi;
    private String trieuChung;
    private String chanDoan;
    private String ketLuan;
    private String trangThai;

    public LuotKham() {
    }

    public LuotKham(String maPhieuKham, LocalDateTime thoiGianTiepNhan, String tenBacSi,
                    String trieuChung, String chanDoan, String ketLuan, String trangThai) {
        this.maPhieuKham = maPhieuKham;
        this.thoiGianTiepNhan = thoiGianTiepNhan;
        this.tenBacSi = tenBacSi;
        this.trieuChung = trieuChung;
        this.chanDoan = chanDoan;
        this.ketLuan = ketLuan;
        this.trangThai = trangThai;
    }

    public String getMaPhieuKham() {
        return maPhieuKham;
    }

    public void setMaPhieuKham(String maPhieuKham) {
        this.maPhieuKham = maPhieuKham;
    }

    public LocalDateTime getThoiGianTiepNhan() {
        return thoiGianTiepNhan;
    }

    public void setThoiGianTiepNhan(LocalDateTime thoiGianTiepNhan) {
        this.thoiGianTiepNhan = thoiGianTiepNhan;
    }

    public String getTenBacSi() {
        return tenBacSi;
    }

    public void setTenBacSi(String tenBacSi) {
        this.tenBacSi = tenBacSi;
    }

    public String getTrieuChung() {
        return trieuChung;
    }

    public void setTrieuChung(String trieuChung) {
        this.trieuChung = trieuChung;
    }

    public String getChanDoan() {
        return chanDoan;
    }

    public void setChanDoan(String chanDoan) {
        this.chanDoan = chanDoan;
    }

    public String getKetLuan() {
        return ketLuan;
    }

    public void setKetLuan(String ketLuan) {
        this.ketLuan = ketLuan;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public String getTrangThaiHienThi() {
        if ("CHO_KHAM".equals(trangThai)) {
            return "Chờ khám";
        }
        if ("DANG_KHAM".equals(trangThai)) {
            return "Đang khám";
        }
        if ("DA_KET_LUAN".equals(trangThai)) {
            return "Hoàn tất";
        }
        if ("DA_HUY".equals(trangThai)) {
            return "Bỏ khám";
        }
        return trangThai;
    }
}
