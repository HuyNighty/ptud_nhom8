package com.quanlynhakhoa.entity;



/** Dữ liệu nghiệp vụ theo schema hiện có; Service/DAO điều phối lưu và chuyển trạng thái. */
public final class ChiTietPhieuXuat {
    private final String maPhieuXuat;
    private final String maLo;
    private final int soLuong;

    public ChiTietPhieuXuat(String maPhieuXuat, String maLo, int soLuong) {
        this.maPhieuXuat = maPhieuXuat;
        this.maLo = maLo;
        this.soLuong = soLuong;
    }

    public String getMaPhieuXuat() { return maPhieuXuat; }
    public String getMaLo() { return maLo; }
    public int getSoLuong() { return soLuong; }
}
