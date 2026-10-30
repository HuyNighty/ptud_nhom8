package com.quanlynhakhoa.billing.service;

import com.quanlynhakhoa.billing.model.*;
import java.util.*;

/** Projection of existing invoice data; no separate receivables storage or invented due dates. */
public final class DebtService {
    private DebtService(){ }
    public static List<HoaDon> outstanding(List<HoaDon> invoices){
        List<HoaDon> result=new ArrayList<>();
        for(HoaDon h:invoices)if(h.trangThai()!=TrangThaiHoaDon.DA_HUY&&h.conNo().signum()>0)result.add(h);
        return List.copyOf(result);
    }
    public static List<PatientDebt> byPatient(List<HoaDon> invoices){
        Map<String,List<HoaDon>> grouped=new LinkedHashMap<>();
        for(HoaDon h:outstanding(invoices)){List<HoaDon> rows=grouped.get(h.maBenhNhan());if(rows==null){rows=new ArrayList<>();grouped.put(h.maBenhNhan(),rows);}rows.add(h);}
        List<PatientDebt> result=new ArrayList<>();
        for(var entry:grouped.entrySet()){
            java.math.BigDecimal total=java.math.BigDecimal.ZERO,paid=java.math.BigDecimal.ZERO,debt=java.math.BigDecimal.ZERO;
            for(HoaDon h:entry.getValue()){total=total.add(h.tongTien());paid=paid.add(h.daThu());debt=debt.add(h.conNo());}
            result.add(new PatientDebt(entry.getKey(),entry.getValue().getFirst().tenBenhNhan(),total,paid,debt,entry.getValue()));
        }
        return List.copyOf(result);
    }
}
