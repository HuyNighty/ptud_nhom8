package com.quanlynhakhoa.nghiepvu.hoadon.service;

import com.quanlynhakhoa.nghiepvu.hoadon.model.*;
import java.util.*;

/** Projection of existing invoice data; no separate receivables storage or invented due dates. */
public final class DebtService {
    private DebtService(){ }
    public static List<Invoice> outstanding(List<Invoice> invoices){
        List<Invoice> result=new ArrayList<>();
        for(Invoice h:invoices)if(h.trangThai()!=InvoiceStatus.DA_HUY&&h.conNo().signum()>0)result.add(h);
        return List.copyOf(result);
    }
    public static List<PatientDebt> byPatient(List<Invoice> invoices){
        Map<String,List<Invoice>> grouped=new LinkedHashMap<>();
        for(Invoice h:outstanding(invoices)){List<Invoice> rows=grouped.get(h.maBenhNhan());if(rows==null){rows=new ArrayList<>();grouped.put(h.maBenhNhan(),rows);}rows.add(h);}
        List<PatientDebt> result=new ArrayList<>();
        for(var entry:grouped.entrySet()){
            java.math.BigDecimal total=java.math.BigDecimal.ZERO,paid=java.math.BigDecimal.ZERO,debt=java.math.BigDecimal.ZERO;
            for(Invoice h:entry.getValue()){total=total.add(h.tongTien());paid=paid.add(h.daThu());debt=debt.add(h.conNo());}
            result.add(new PatientDebt(entry.getKey(),entry.getValue().getFirst().tenBenhNhan(),total,paid,debt,entry.getValue()));
        }
        return List.copyOf(result);
    }
}
