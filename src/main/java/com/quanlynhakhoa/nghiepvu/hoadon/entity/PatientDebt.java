package com.quanlynhakhoa.nghiepvu.hoadon.entity;

import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import java.math.BigDecimal;
import java.util.List;

public record PatientDebt(String patientId,String name,BigDecimal total,BigDecimal collected,BigDecimal debt,List<ThongTinHoaDon> invoices) {
    public PatientDebt{invoices=List.copyOf(invoices);}
}
