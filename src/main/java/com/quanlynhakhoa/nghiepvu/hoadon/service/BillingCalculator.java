package com.quanlynhakhoa.nghiepvu.hoadon.service;

import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.model.hoadon.DongChiTietHoaDon;
import com.quanlynhakhoa.nghiepvu.hoadon.entity.*;
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
            if (payment.trangThai() == ThongTinThanhToan.TrangThai.THANH_CONG) total = total.add(payment.soTien());
        }
        return total;
    }
    public static BigDecimal patientDebt(List<Invoice> invoices) {
        BigDecimal total = BigDecimal.ZERO;
        for (Invoice invoice : invoices) total = total.add(invoice.noTheoDoi());
        return total;
    }
}
