package com.quanlynhakhoa.inventory.thuoc.model;

import java.time.LocalDate;

/** Dữ liệu danh mục thuốc; tồn và hạn gần nhất là dữ liệu tổng hợp chỉ đọc từ LoHang. */
public class Thuoc {
    private String maThuoc;
    private String tenThuoc;
    private String donViTinh;
    private boolean kiemSoatDacBiet;
    private String hamLuong;
    private String hoatChat;
    private int dinhMucToiThieu;
    private boolean daDatHang;
    private long tongTon;
    private LocalDate hanSuDungGanNhat;

    public Thuoc() { }

    public Thuoc(String maThuoc, String tenThuoc, String donViTinh,
                 boolean kiemSoatDacBiet, String hamLuong, String hoatChat,
                 int dinhMucToiThieu, boolean daDatHang) {
        this.maThuoc = maThuoc;
        this.tenThuoc = tenThuoc;
        this.donViTinh = donViTinh;
        this.kiemSoatDacBiet = kiemSoatDacBiet;
        this.hamLuong = hamLuong;
        this.hoatChat = hoatChat;
        this.dinhMucToiThieu = dinhMucToiThieu;
        this.daDatHang = daDatHang;
    }

    public String getMaThuoc() { return maThuoc; }
    public void setMaThuoc(String maThuoc) { this.maThuoc = maThuoc; }
    public String getTenThuoc() { return tenThuoc; }
    public void setTenThuoc(String tenThuoc) { this.tenThuoc = tenThuoc; }
    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }
    public boolean isKiemSoatDacBiet() { return kiemSoatDacBiet; }
    public void setKiemSoatDacBiet(boolean kiemSoatDacBiet) { this.kiemSoatDacBiet = kiemSoatDacBiet; }
    public String getHamLuong() { return hamLuong; }
    public void setHamLuong(String hamLuong) { this.hamLuong = hamLuong; }
    public String getHoatChat() { return hoatChat; }
    public void setHoatChat(String hoatChat) { this.hoatChat = hoatChat; }
    public int getDinhMucToiThieu() { return dinhMucToiThieu; }
    public void setDinhMucToiThieu(int dinhMucToiThieu) { this.dinhMucToiThieu = dinhMucToiThieu; }
    public boolean isDaDatHang() { return daDatHang; }
    public void setDaDatHang(boolean daDatHang) { this.daDatHang = daDatHang; }
    public long getTongTon() { return tongTon; }
    public void setTongTon(long tongTon) { this.tongTon = tongTon; }
    public LocalDate getHanSuDungGanNhat() { return hanSuDungGanNhat; }
    public void setHanSuDungGanNhat(LocalDate hanSuDungGanNhat) { this.hanSuDungGanNhat = hanSuDungGanNhat; }
}
