package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class LoHang {
    private String maLo;
    private String soLo;
    private LocalDate ngaySanXuat;
    private LocalDate hanSuDung;
    private String maThuoc;
    private String maVatTu;
    private String maNhaCungCap;
    private int soLuongTon;

    public LoHang() {
        this(null, null, null, null, null, null, null, 0);
    }

    public LoHang(String maLo, String soLo, LocalDate ngaySanXuat, LocalDate hanSuDung, String maThuoc, String maVatTu, String maNhaCungCap, int soLuongTon) {
        this.maLo = maLo;
        this.soLo = soLo;
        this.ngaySanXuat = ngaySanXuat;
        this.hanSuDung = hanSuDung;
        this.maThuoc = maThuoc;
        this.maVatTu = maVatTu;
        this.maNhaCungCap = maNhaCungCap;
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
    public String getMaNhaCungCap() { return maNhaCungCap; }
    public void setMaNhaCungCap(String maNhaCungCap) { this.maNhaCungCap = maNhaCungCap; }
    public int getSoLuongTon() { return soLuongTon; }
    public void setSoLuongTon(int soLuongTon) { this.soLuongTon = soLuongTon; }
}
