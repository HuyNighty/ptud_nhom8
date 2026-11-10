package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongChiTietHoaDon {
    private final int maChiTiet;
    private final String maHoaDon;
    private final String maDichVu;
    private final String maThuoc;
    private final String maVatTu;
    private final int soLuong;
    private final BigDecimal donGiaApDung;
    private final String tenHang;

    public DongChiTietHoaDon(int maChiTiet, String maHoaDon, String maDichVu, String maThuoc, String maVatTu, int soLuong, BigDecimal donGiaApDung, String tenHang) {
        int kinds = (maDichVu == null ? 0 : 1) + (maThuoc == null ? 0 : 1) + (maVatTu == null ? 0 : 1);
        if (kinds != 1 || soLuong <= 0 || donGiaApDung == null || donGiaApDung.signum() < 0
        || donGiaApDung.stripTrailingZeros().scale() > 0) {
        throw new IllegalArgumentException("Chi tiết hóa đơn bất nhất: loại hàng, số lượng hoặc giá chốt.");
        }
        this.maChiTiet = maChiTiet;
        this.maHoaDon = maHoaDon;
        this.maDichVu = maDichVu;
        this.maThuoc = maThuoc;
        this.maVatTu = maVatTu;
        this.soLuong = soLuong;
        this.donGiaApDung = donGiaApDung;
        this.tenHang = tenHang;
    }

    public int maChiTiet() { return maChiTiet; }
    public String maHoaDon() { return maHoaDon; }
    public String maDichVu() { return maDichVu; }
    public String maThuoc() { return maThuoc; }
    public String maVatTu() { return maVatTu; }
    public int soLuong() { return soLuong; }
    public BigDecimal donGiaApDung() { return donGiaApDung; }
    public String tenHang() { return tenHang; }

    public DongChiTietHoaDon(int id,String invoice,String service,String medicine,String material,int quantity,BigDecimal price){
        this(id,invoice,service,medicine,material,quantity,price,null);
    }

    public BigDecimal thanhTien() { return donGiaApDung.multiply(BigDecimal.valueOf(soLuong)); }
    public String loaiHang() { return maDichVu != null ? "Dịch vụ" : maThuoc != null ? "Thuốc" : "Vật tư"; }
    public String maHang() { return maDichVu != null ? maDichVu : maThuoc != null ? maThuoc : maVatTu; }
    public String tenHienThi() { return tenHang==null||tenHang.isBlank()?maHang():tenHang; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongChiTietHoaDon value)) return false;
        return maChiTiet == value.maChiTiet
                && java.util.Objects.equals(maHoaDon, value.maHoaDon)
                && java.util.Objects.equals(maDichVu, value.maDichVu)
                && java.util.Objects.equals(maThuoc, value.maThuoc)
                && java.util.Objects.equals(maVatTu, value.maVatTu)
                && soLuong == value.soLuong
                && java.util.Objects.equals(donGiaApDung, value.donGiaApDung)
                && java.util.Objects.equals(tenHang, value.tenHang);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Integer.hashCode(maChiTiet);
        result = 31 * result + java.util.Objects.hashCode(maHoaDon);
        result = 31 * result + java.util.Objects.hashCode(maDichVu);
        result = 31 * result + java.util.Objects.hashCode(maThuoc);
        result = 31 * result + java.util.Objects.hashCode(maVatTu);
        result = 31 * result + Integer.hashCode(soLuong);
        result = 31 * result + java.util.Objects.hashCode(donGiaApDung);
        result = 31 * result + java.util.Objects.hashCode(tenHang);
        return result;
    }

    @Override
    public String toString() {
        return "InvoiceLine[" + "maChiTiet=" + maChiTiet + ", " + "maHoaDon=" + maHoaDon + ", " + "maDichVu=" + maDichVu + ", " + "maThuoc=" + maThuoc + ", " + "maVatTu=" + maVatTu + ", " + "soLuong=" + soLuong + ", " + "donGiaApDung=" + donGiaApDung + ", " + "tenHang=" + tenHang + "]";
    }
}
