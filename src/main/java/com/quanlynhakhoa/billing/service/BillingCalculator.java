package com.quanlynhakhoa.billing.service;

import com.quanlynhakhoa.billing.model.*;
import java.math.BigDecimal;
import java.util.List;

public final class BillingCalculator {
    private BillingCalculator() { }
    public static BigDecimal total(List<ChiTietHoaDon> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ChiTietHoaDon line : lines) total = total.add(line.thanhTien());
        return total;
    }
    public static BigDecimal collected(List<ThanhToan> payments) {
        BigDecimal total = BigDecimal.ZERO;
        for (ThanhToan payment : payments) {
            if (payment.trangThai() == ThanhToan.TrangThai.THANH_CONG) total = total.add(payment.soTien());
        }
        return total;
    }
    public static BigDecimal patientDebt(List<HoaDon> invoices) {
        BigDecimal total = BigDecimal.ZERO;
        for (HoaDon invoice : invoices) total = total.add(invoice.noTheoDoi());
        return total;
    }
}
