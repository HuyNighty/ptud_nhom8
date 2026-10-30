package com.quanlynhakhoa.lichhen.model;

import java.time.LocalDateTime;

public class LichHen {
    public static final String CHO_XAC_NHAN = "CHO_XAC_NHAN";
    public static final String DA_XAC_NHAN = "DA_XAC_NHAN";
    public static final String DA_DEN = "DA_DEN";
    public static final String DA_HUY = "DA_HUY";
    public static final String DA_DOI_LICH = "DA_DOI_LICH";

    private String maLichHen;
    private LocalDateTime thoiGianHen;
    private String maBenhNhan;
    private String tenBenhNhan;
    private String soDienThoaiBenhNhan;
    private String maBacSi;
    private String tenBacSi;
    private String lyDoHen;
    private String trangThai;
    private String maLichHenGoc;
    private String lyDoHuy;

    public LichHen() {
    }

    public LichHen(String maLichHen) {
        this.maLichHen = maLichHen;
    }

    public String getMaLichHen() {
        return maLichHen;
    }

    public void setMaLichHen(String maLichHen) {
        this.maLichHen = maLichHen;
    }

    public LocalDateTime getThoiGianHen() {
        return thoiGianHen;
    }

    public void setThoiGianHen(LocalDateTime thoiGianHen) {
        this.thoiGianHen = thoiGianHen;
    }

    public String getMaBenhNhan() {
        return maBenhNhan;
    }

    public void setMaBenhNhan(String maBenhNhan) {
        this.maBenhNhan = maBenhNhan;
    }

    public String getTenBenhNhan() {
        return tenBenhNhan;
    }

    public void setTenBenhNhan(String tenBenhNhan) {
        this.tenBenhNhan = tenBenhNhan;
    }

    public String getSoDienThoaiBenhNhan() {
        return soDienThoaiBenhNhan;
    }

    public void setSoDienThoaiBenhNhan(String soDienThoaiBenhNhan) {
        this.soDienThoaiBenhNhan = soDienThoaiBenhNhan;
    }

    public String getMaBacSi() {
        return maBacSi;
    }

    public void setMaBacSi(String maBacSi) {
        this.maBacSi = maBacSi;
    }

    public String getTenBacSi() {
        return tenBacSi;
    }

    public void setTenBacSi(String tenBacSi) {
        this.tenBacSi = tenBacSi;
    }

    public String getLyDoHen() {
        return lyDoHen;
    }

    public void setLyDoHen(String lyDoHen) {
        this.lyDoHen = lyDoHen;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public String getMaLichHenGoc() {
        return maLichHenGoc;
    }

    public void setMaLichHenGoc(String maLichHenGoc) {
        this.maLichHenGoc = maLichHenGoc;
    }

    public String getLyDoHuy() {
        return lyDoHuy;
    }

    public void setLyDoHuy(String lyDoHuy) {
        this.lyDoHuy = lyDoHuy;
    }

    public boolean laDaDat() {
        if (CHO_XAC_NHAN.equals(trangThai)) {
            return true;
        }
        if (DA_XAC_NHAN.equals(trangThai)) {
            return true;
        }
        return false;
    }

    public boolean duocTiepDon() {
        if (!laDaDat()) {
            return false;
        }
        return thoiGianHen.toLocalDate().equals(java.time.LocalDate.now());
    }

    public String getTrangThaiHienThi() {
        if (CHO_XAC_NHAN.equals(trangThai)) {
            return "Đã đặt";
        }
        if (DA_XAC_NHAN.equals(trangThai)) {
            return "Đã đặt (đã xác nhận)";
        }
        if (DA_DEN.equals(trangThai)) {
            return "Đã đến";
        }
        if ("HOAN_THANH".equals(trangThai)) {
            return "Hoàn thành";
        }
        if ("VANG_MAT".equals(trangThai)) {
            return "Vắng mặt";
        }
        if (DA_HUY.equals(trangThai)) {
            return "Đã hủy";
        }
        if (DA_DOI_LICH.equals(trangThai)) {
            return "Đã đổi lịch";
        }
        return trangThai;
    }
}
