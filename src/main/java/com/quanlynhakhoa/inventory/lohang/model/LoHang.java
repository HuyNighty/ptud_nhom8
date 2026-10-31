package com.quanlynhakhoa.inventory.lohang.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Model chỉ đọc lô thuốc hoặc vật tư; số tồn chỉ thay đổi qua chứng từ kho. */
public class LoHang {
    public static final String LOAI_TAT_CA = "ALL";
    public static final String LOAI_THUOC = "THUOC";
    public static final String LOAI_VAT_TU = "VATTU";
    public static final String HAN_TAT_CA = "ALL";
    public static final String HAN_HET_HAN = "HET_HAN";
    public static final String HAN_CON_HAN = "CON_HAN";

    private String maLo;
    private String soLo;
    private LocalDate ngaySanXuat;
    private LocalDate hanSuDung;
    private String maThuoc;
    private String maVatTu;
    private String tenHang;
    private String maNhaCungCap;
    private String tenNhaCungCap;
    private int soLuongTon;

    public LoHang() { }

    public LoHang(String maLo, String soLo, LocalDate ngaySanXuat, LocalDate hanSuDung,
                  String maThuoc, String maVatTu, String tenHang,
                  String maNhaCungCap, String tenNhaCungCap, int soLuongTon) {
        this.maLo = maLo;
        this.soLo = soLo;
        this.ngaySanXuat = ngaySanXuat;
        this.hanSuDung = hanSuDung;
        this.maThuoc = maThuoc;
        this.maVatTu = maVatTu;
        this.tenHang = tenHang;
        this.maNhaCungCap = maNhaCungCap;
        this.tenNhaCungCap = tenNhaCungCap;
        this.soLuongTon = soLuongTon;
    }

    public String getMaLo() { return maLo; }
    public void setMaLo(String maLo) { this.maLo = maLo; }
    public String getSoLo() { return soLo; }
    public void setSoLo(String soLo) { this.soLo = soLo; }
    public LocalDate getNgaySanXuat() { return ngaySanXuat; }
    public void setNgaySanXuat(LocalDate ngaySanXuat) { this.ngaySanXuat = ngaySanXuat; }
    public LocalDate getHanSuDung() { return hanSuDung; }
    public void setHanSuDung(LocalDate hanSuDung) { this.hanSuDung = hanSuDung; }
    public String getMaThuoc() { return maThuoc; }
    public void setMaThuoc(String maThuoc) { this.maThuoc = maThuoc; }
    public String getMaVatTu() { return maVatTu; }
    public void setMaVatTu(String maVatTu) { this.maVatTu = maVatTu; }
    public String getTenHang() { return tenHang; }
    public void setTenHang(String tenHang) { this.tenHang = tenHang; }
    public String getMaNhaCungCap() { return maNhaCungCap; }
    public void setMaNhaCungCap(String maNhaCungCap) { this.maNhaCungCap = maNhaCungCap; }
    public String getTenNhaCungCap() { return tenNhaCungCap; }
    public void setTenNhaCungCap(String tenNhaCungCap) { this.tenNhaCungCap = tenNhaCungCap; }
    public int getSoLuongTon() { return soLuongTon; }
    public void setSoLuongTon(int soLuongTon) { this.soLuongTon = soLuongTon; }

    public String getLoaiHangHienThi() { return maThuoc != null ? "Thuốc" : "Vật tư"; }

    public boolean daHetHan(LocalDate ngay) {
        return hanSuDung != null && hanSuDung.isBefore(ngay);
    }

    public String getTinhTrangHan() {
        if (hanSuDung == null) return "Không có HSD";
        return daHetHan(LocalDate.now()) ? "Đã hết hạn" : "Còn hạn";
    }

    public long getSoNgayConLai(LocalDate ngay) {
        return hanSuDung == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(ngay, hanSuDung);
    }
}
