package com.quanlynhakhoa.nghiepvu.hoadon.entity;

import java.math.BigDecimal;
import java.util.List;

public record PatientDebt(String patientId,String name,BigDecimal total,BigDecimal collected,BigDecimal debt,List<Invoice> invoices) {
    public PatientDebt{invoices=List.copyOf(invoices);}
}
