package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.entity.ChiTietHoaDon;

import java.math.BigDecimal;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongChiTietHoaDon {
    private final ChiTietHoaDon chiTietHoaDon;
    private final String tenHang;

    public DongChiTietHoaDon(int maChiTiet, String maHoaDon, String maDichVu, String maThuoc, String maVatTu, int soLuong, BigDecimal donGiaApDung, String tenHang) {
        int kinds = (maDichVu == null ? 0 : 1) + (maThuoc == null ? 0 : 1) + (maVatTu == null ? 0 : 1);
        if (kinds != 1 || soLuong <= 0 || donGiaApDung == null || donGiaApDung.signum() < 0
        || donGiaApDung.stripTrailingZeros().scale() > 0) {
        throw new IllegalArgumentException("Chi tiết hóa đơn bất nhất: loại hàng, số lượng hoặc giá chốt.");
        }
        this.chiTietHoaDon = new ChiTietHoaDon(maChiTiet, maHoaDon, maDichVu, maThuoc, maVatTu, soLuong, donGiaApDung);






        this.tenHang = tenHang;
    }

    public int maChiTiet() { return chiTietHoaDon.getMaChiTiet(); }
    public String maHoaDon() { return chiTietHoaDon.getMaHoaDon(); }
    public String maDichVu() { return chiTietHoaDon.getMaDichVu(); }
    public String maThuoc() { return chiTietHoaDon.getMaThuoc(); }
    public String maVatTu() { return chiTietHoaDon.getMaVatTu(); }
    public int soLuong() { return chiTietHoaDon.getSoLuong(); }
    public BigDecimal donGiaApDung() { return chiTietHoaDon.getDonGiaApDung(); }
    public String tenHang() { return tenHang; }

    public DongChiTietHoaDon(int id,String invoice,String service,String medicine,String material,int quantity,BigDecimal price){
        this(id,invoice,service,medicine,material,quantity,price,null);
    }

    public BigDecimal thanhTien() { return chiTietHoaDon.getDonGiaApDung().multiply(BigDecimal.valueOf(chiTietHoaDon.getSoLuong())); }
    public String loaiHang() { return chiTietHoaDon.getMaDichVu() != null ? "Dịch vụ" : chiTietHoaDon.getMaThuoc() != null ? "Thuốc" : "Vật tư"; }
    public String maHang() { return chiTietHoaDon.getMaDichVu() != null ? chiTietHoaDon.getMaDichVu() : chiTietHoaDon.getMaThuoc() != null ? chiTietHoaDon.getMaThuoc() : chiTietHoaDon.getMaVatTu(); }
    public String tenHienThi() { return tenHang==null||tenHang.isBlank()?maHang():tenHang; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongChiTietHoaDon value)) return false;
        return chiTietHoaDon.getMaChiTiet() == value.chiTietHoaDon.getMaChiTiet()
                && java.util.Objects.equals(chiTietHoaDon.getMaHoaDon(), value.chiTietHoaDon.getMaHoaDon())
                && java.util.Objects.equals(chiTietHoaDon.getMaDichVu(), value.chiTietHoaDon.getMaDichVu())
                && java.util.Objects.equals(chiTietHoaDon.getMaThuoc(), value.chiTietHoaDon.getMaThuoc())
                && java.util.Objects.equals(chiTietHoaDon.getMaVatTu(), value.chiTietHoaDon.getMaVatTu())
                && chiTietHoaDon.getSoLuong() == value.chiTietHoaDon.getSoLuong()
                && java.util.Objects.equals(chiTietHoaDon.getDonGiaApDung(), value.chiTietHoaDon.getDonGiaApDung())
                && java.util.Objects.equals(tenHang, value.tenHang);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Integer.hashCode(chiTietHoaDon.getMaChiTiet());
        result = 31 * result + java.util.Objects.hashCode(chiTietHoaDon.getMaHoaDon());
        result = 31 * result + java.util.Objects.hashCode(chiTietHoaDon.getMaDichVu());
        result = 31 * result + java.util.Objects.hashCode(chiTietHoaDon.getMaThuoc());
        result = 31 * result + java.util.Objects.hashCode(chiTietHoaDon.getMaVatTu());
        result = 31 * result + Integer.hashCode(chiTietHoaDon.getSoLuong());
        result = 31 * result + java.util.Objects.hashCode(chiTietHoaDon.getDonGiaApDung());
        result = 31 * result + java.util.Objects.hashCode(tenHang);
        return result;
    }

    @Override
    public String toString() {
        return "InvoiceLine[" + "maChiTiet=" + chiTietHoaDon.getMaChiTiet() + ", " + "maHoaDon=" + chiTietHoaDon.getMaHoaDon() + ", " + "maDichVu=" + chiTietHoaDon.getMaDichVu() + ", " + "maThuoc=" + chiTietHoaDon.getMaThuoc() + ", " + "maVatTu=" + chiTietHoaDon.getMaVatTu() + ", " + "soLuong=" + chiTietHoaDon.getSoLuong() + ", " + "donGiaApDung=" + chiTietHoaDon.getDonGiaApDung() + ", " + "tenHang=" + tenHang + "]";
    }

    public ChiTietHoaDon getChiTietHoaDon() { return chiTietHoaDon; }
}
