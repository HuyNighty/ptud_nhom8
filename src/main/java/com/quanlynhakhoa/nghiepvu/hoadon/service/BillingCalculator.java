package com.quanlynhakhoa.nghiepvu.hoadon.service;

import com.quanlynhakhoa.nghiepvu.hoadon.entity.*;
import java.math.BigDecimal;
import java.util.List;

public final class BillingCalculator {
    private BillingCalculator() { }
    public static BigDecimal total(List<InvoiceLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLine line : lines) total = total.add(line.thanhTien());
        return total;
    }
    public static BigDecimal collected(List<Payment> payments) {
        BigDecimal total = BigDecimal.ZERO;
        for (Payment payment : payments) {
            if (payment.trangThai() == Payment.TrangThai.THANH_CONG) total = total.add(payment.soTien());
        }
        return total;
    }
    public static BigDecimal patientDebt(List<Invoice> invoices) {
        BigDecimal total = BigDecimal.ZERO;
        for (Invoice invoice : invoices) total = total.add(invoice.noTheoDoi());
        return total;
    }
}
