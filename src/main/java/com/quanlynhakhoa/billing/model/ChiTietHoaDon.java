package com.quanlynhakhoa.billing.model;

import java.math.BigDecimal;

public record ChiTietHoaDon(int maChiTiet, String maHoaDon, String maDichVu, String maThuoc,
                            String maVatTu, int soLuong, BigDecimal donGiaApDung) {
    public ChiTietHoaDon {
        int kinds = (maDichVu == null ? 0 : 1) + (maThuoc == null ? 0 : 1) + (maVatTu == null ? 0 : 1);
        if (kinds != 1 || soLuong <= 0 || donGiaApDung == null || donGiaApDung.signum() < 0
                || donGiaApDung.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Chi tiết hóa đơn bất nhất: loại hàng, số lượng hoặc giá chốt.");
        }
    }
    public BigDecimal thanhTien() { return donGiaApDung.multiply(BigDecimal.valueOf(soLuong)); }
    public String loaiHang() { return maDichVu != null ? "Dịch vụ" : maThuoc != null ? "Thuốc" : "Vật tư"; }
    public String maHang() { return maDichVu != null ? maDichVu : maThuoc != null ? maThuoc : maVatTu; }
}
