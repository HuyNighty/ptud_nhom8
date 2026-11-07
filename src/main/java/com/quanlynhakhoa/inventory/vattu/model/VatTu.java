package com.quanlynhakhoa.inventory.vattu.model;

import java.time.LocalDate;

/** Dữ liệu danh mục vật tư; tồn kho và hạn gần nhất chỉ đọc từ các lô hàng. */
public class VatTu {
    private String maVatTu;
    private String tenVatTu;
    private String donViTinh;
    private String moTa;
    private int dinhMucToiThieu;
    private boolean daDatHang;
    private long tongTon;
    private LocalDate hanSuDungGanNhat;

    public VatTu() { }

    public VatTu(String maVatTu, String tenVatTu, String donViTinh, String moTa,
                 int dinhMucToiThieu, boolean daDatHang) {
        this.maVatTu = maVatTu;
        this.tenVatTu = tenVatTu;
        this.donViTinh = donViTinh;
        this.moTa = moTa;
        this.dinhMucToiThieu = dinhMucToiThieu;
        this.daDatHang = daDatHang;
    }

    public String getMaVatTu() { return maVatTu; }
    public void setMaVatTu(String maVatTu) { this.maVatTu = maVatTu; }
    public String getTenVatTu() { return tenVatTu; }
    public void setTenVatTu(String tenVatTu) { this.tenVatTu = tenVatTu; }
    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public int getDinhMucToiThieu() { return dinhMucToiThieu; }
    public void setDinhMucToiThieu(int dinhMucToiThieu) { this.dinhMucToiThieu = dinhMucToiThieu; }
    public boolean isDaDatHang() { return daDatHang; }
    public void setDaDatHang(boolean daDatHang) { this.daDatHang = daDatHang; }
    public long getTongTon() { return tongTon; }
    public void setTongTon(long tongTon) { this.tongTon = tongTon; }
    public LocalDate getHanSuDungGanNhat() { return hanSuDungGanNhat; }
    public void setHanSuDungGanNhat(LocalDate hanSuDungGanNhat) { this.hanSuDungGanNhat = hanSuDungGanNhat; }
}
