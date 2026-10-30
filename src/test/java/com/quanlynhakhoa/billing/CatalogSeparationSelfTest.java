package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.dichvu.model.DichVu;
import com.quanlynhakhoa.dichvu.service.DichVuService;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.*;
import java.math.BigDecimal;
import static com.quanlynhakhoa.billing.ServiceBillingSelfTest.*;

public final class CatalogSeparationSelfTest {
    public static void main(String[] args)throws Exception {
        final FeatureJdbcDouble jdbc=new FeatureJdbcDouble();final DesktopSession principal=session(Role.CLINIC_MANAGER);
        final DichVuService service=new DichVuService(principal,jdbc);
        final DichVu value=new DichVu("DV1","Name",new BigDecimal("9999"),"Description",true,true);
        service.saveInformation(value,false);
        check(jdbc.sql.getFirst().contains("SET tenDichVu=")&&!jdbc.sql.getFirst().contains("donGia="),"information updates cannot overwrite catalogue price");
        int before=jdbc.sql.size();service.savePrice(value);
        check(jdbc.sql.get(before).contains("SET donGia=")&&!jdbc.sql.get(before).contains("tenDichVu="),"price updates cannot overwrite service metadata");
        check(jdbc.audits.size()==2&&jdbc.commits==2,"both modes audit transactionally");
        final DichVuService denied=new DichVuService(session(Role.RECEPTIONIST),jdbc);before=jdbc.opens;
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{denied.savePrice(value);}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{denied.saveInformation(value,false);}});
        check(jdbc.opens==before,"denied before connection");
        principal.logout();before=jdbc.opens;
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{service.savePrice(value);}});
        expect(AccessDeniedException.class,new Action(){public void run()throws Exception{service.saveInformation(value,true);}});
        check(jdbc.opens==before,"both modes blocked after logout");
        System.out.println("PASS: CatalogSeparationSelfTest (separate UPDATE fields, permissions and audit; synthetic JDBC).");
    }
}
