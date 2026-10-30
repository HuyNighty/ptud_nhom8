package com.quanlynhakhoa.billing.model;

import java.util.List;

public record ChiTietBilling(HoaDon hoaDon, List<ChiTietHoaDon> chiTiet, List<ThanhToan> thanhToan) {
    public ChiTietBilling { chiTiet = List.copyOf(chiTiet); thanhToan = List.copyOf(thanhToan); }
}
