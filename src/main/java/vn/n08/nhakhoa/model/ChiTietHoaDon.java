package vn.n08.nhakhoa.model;

import java.math.BigDecimal;

/** Bảng ChiTietHoaDon — dòng dịch vụ, thuốc hoặc vật tư với đơn giá tại thời điểm lập. */
public record ChiTietHoaDon(
        Integer maChiTiet,
        String maHoaDon,
        String maDichVu,
        String maThuoc,
        String maVatTu,
        int soLuong,
        BigDecimal donGiaApDung) {
    public BigDecimal thanhTien() { return donGiaApDung.multiply(BigDecimal.valueOf(soLuong)); }
}
