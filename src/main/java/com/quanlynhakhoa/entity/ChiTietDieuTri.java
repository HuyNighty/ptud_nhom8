package com.quanlynhakhoa.entity;

import java.time.LocalDate;

/** Dữ liệu nghiệp vụ theo schema hiện có; Service/DAO điều phối lưu và chuyển trạng thái. */
public final class ChiTietDieuTri {
    private final int maChiTiet;
    private final String maKeHoach;
    private final String maDichVu;
    private String viTriRang;
    private LocalDate ngayDuKien;
    private final LocalDate ngayThucHien;
    private final String ketQua;
    private final String trangThai;
    private final String maNhanVienThucHien;
    private String ghiChu;

    public ChiTietDieuTri(int maChiTiet, String maKeHoach, String maDichVu, String viTriRang, LocalDate ngayDuKien, LocalDate ngayThucHien, String ketQua, String trangThai, String maNhanVienThucHien, String ghiChu) {
        this.maChiTiet = maChiTiet;
        this.maKeHoach = maKeHoach;
        this.maDichVu = maDichVu;
        this.viTriRang = viTriRang;
        this.ngayDuKien = ngayDuKien;
        this.ngayThucHien = ngayThucHien;
        this.ketQua = ketQua;
        this.trangThai = trangThai;
        this.maNhanVienThucHien = maNhanVienThucHien;
        this.ghiChu = ghiChu;
    }

    public int getMaChiTiet() { return maChiTiet; }
    public String getMaKeHoach() { return maKeHoach; }
    public String getMaDichVu() { return maDichVu; }
    public String getViTriRang() { return viTriRang; }
    public void setViTriRang(String viTriRang) { this.viTriRang = viTriRang; }
    public LocalDate getNgayDuKien() { return ngayDuKien; }
    public void setNgayDuKien(LocalDate ngayDuKien) { this.ngayDuKien = ngayDuKien; }
    public LocalDate getNgayThucHien() { return ngayThucHien; }
    public String getKetQua() { return ketQua; }
    public String getTrangThai() { return trangThai; }
    public String getMaNhanVienThucHien() { return maNhanVienThucHien; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
