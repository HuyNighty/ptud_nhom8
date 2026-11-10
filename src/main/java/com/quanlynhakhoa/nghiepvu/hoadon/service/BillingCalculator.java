package com.quanlynhakhoa.nghiepvu.hoadon.service;

import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.model.hoadon.DongChiTietHoaDon;
import java.math.BigDecimal;
import java.util.List;

public final class BillingCalculator {
    private BillingCalculator() { }
    public static BigDecimal total(List<DongChiTietHoaDon> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (DongChiTietHoaDon line : lines) total = total.add(line.thanhTien());
        return total;
    }
    public static BigDecimal collected(List<ThongTinThanhToan> payments) {
        BigDecimal total = BigDecimal.ZERO;
        for (ThongTinThanhToan payment : payments) {
            if (payment.trangThai() == TrangThaiThanhToan.THANH_CONG) total = total.add(payment.soTien());
        }
        return total;
    }
    public static BigDecimal patientDebt(List<ThongTinHoaDon> invoices) {
        BigDecimal total = BigDecimal.ZERO;
        for (ThongTinHoaDon invoice : invoices) total = total.add(invoice.noTheoDoi());
        return total;
    }
}
