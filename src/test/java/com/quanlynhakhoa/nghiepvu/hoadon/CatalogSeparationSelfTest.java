package com.quanlynhakhoa.nghiepvu.hoadon;



import com.quanlynhakhoa.entity.DichVuNhaKhoa;
import com.quanlynhakhoa.nghiepvu.dichvu.service.ServiceCatalogService;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.math.BigDecimal;
import static com.quanlynhakhoa.nghiepvu.hoadon.ServiceBillingSelfTest.*;

public final class CatalogSeparationSelfTest {
    public static void main(String[] args)throws Exception {
        final FeatureJdbcDouble jdbc=new FeatureJdbcDouble();final DesktopSession principal=session(VaiTroHeThong.CLINIC_MANAGER);
        final ServiceCatalogService service=new ServiceCatalogService(principal,jdbc);
        final DichVuNhaKhoa value=new DichVuNhaKhoa("DV1","Name",new BigDecimal("9999"),"Description",true,true);
        service.saveInformation(value,false);
        check(jdbc.sql.getFirst().contains("SET tenDichVu=")&&!jdbc.sql.getFirst().contains("donGia="),"information updates cannot overwrite catalogue price");
        int before=jdbc.sql.size();service.savePrice(value);
        check(jdbc.sql.get(before).contains("SET donGia=")&&!jdbc.sql.get(before).contains("tenDichVu="),"price updates cannot overwrite service metadata");
        check(jdbc.audits.size()==2&&jdbc.commits==2,"both modes audit transactionally");
        final ServiceCatalogService denied=new ServiceCatalogService(session(VaiTroHeThong.RECEPTIONIST),jdbc);before=jdbc.opens;
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
