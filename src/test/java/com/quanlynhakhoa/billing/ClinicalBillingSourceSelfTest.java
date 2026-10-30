package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.billing.dao.ClinicalChargeDao.Evidence;
import com.quanlynhakhoa.billing.model.InvoiceDraft;
import com.quanlynhakhoa.billing.service.*;
import java.math.BigDecimal;
import java.util.*;
import static com.quanlynhakhoa.billing.ServiceBillingSelfTest.*;

public final class ClinicalBillingSourceSelfTest {
    private static Evidence row(String type,String id,String service,String price,boolean active){return new Evidence(type,id,service,"Synthetic",new BigDecimal(price),active,type.equals("CLS"));}
    public static void main(String[] args)throws Exception {
        List<Evidence> rows=List.of(row("CLS","C1","DV1","100",true),row("CLS","C2","DV1","100",true),row("DIEU_TRI","1","DV2","200",false));
        InvoiceDraft draft=JdbcClinicalBillingSource.build("BN1","PK1",rows);
        check(draft.lines().size()==3 && draft.total().intValueExact()==400,"one unit per performed evidence; repeated service in same source charged per instance");
        new JdbcClinicalBillingSource().validateServiceState(null,draft,"DV2",false);
        check(draft.lines().get(2).unitPrice().intValueExact()==200,"stopped historical service retained");
        InvoiceDraft changed=JdbcClinicalBillingSource.build("BN1","PK1",List.of(row("CLS","C1","DV1","101",true)));
        check(!draft.revision().equals(changed.revision()),"trusted price change invalidates reviewed revision");
        for(final List<Evidence> invalid:List.of(List.<Evidence>of(),List.of(rows.get(0),rows.get(0)),List.of(rows.get(0),row("DIEU_TRI","2","DV1","100",true)),List.of(row("CLS","C1","DV1","-1",true)),List.of(row("CLS","C1","DV1","1.5",true)))) {
            expect(IllegalArgumentException.class,new Action(){public void run(){JdbcClinicalBillingSource.build("BN1","PK1",invalid);}});
        }
        RandomInvoiceIdGenerator ids=new RandomInvoiceIdGenerator();Set<String> seen=new HashSet<>();
        for(int i=0;i<10000;i++){String id=ids.next(null);check(id.length()==10&&seen.add(id),"random ID format and sample collision check");}
        System.out.println("PASS: ClinicalBillingSourceSelfTest (contract rules and ID sampling; real clinical SQL acceptance pending).");
    }
}
