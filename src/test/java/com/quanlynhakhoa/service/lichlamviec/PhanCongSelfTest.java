package com.quanlynhakhoa.service.lichlamviec;

import com.quanlynhakhoa.model.lichlamviec.ThongTinPhanCong;
import com.quanlynhakhoa.entity.CaLamViec;
import com.quanlynhakhoa.service.lichlamviec.PhanCongService;
import com.quanlynhakhoa.hethong.phanquyen.*;
import com.quanlynhakhoa.security.KetQuaDangNhap;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.enums.Module;
import com.quanlynhakhoa.hethong.dieuhuong.NavigationPolicy;
import com.quanlynhakhoa.hethong.tinhnang.FeatureBootstrap;
import java.time.*;
import java.util.*;
import java.sql.SQLException;
public final class PhanCongSelfTest {
    private static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    interface Action{void run()throws Exception;}
    static void rejects(Class<? extends Throwable> type,Action a)throws Exception{try{a.run();}catch(Throwable e){check(type.isInstance(e),"Expected "+type+" got "+e);return;}throw new AssertionError("Expected rejection "+type);}
    static DesktopSession session(VaiTroHeThong role){return DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true,"ACCOUNT1",role,false));}
    public static void main(String[] args)throws Exception {
        LocalDate day=LocalDate.now();
        for(VaiTroHeThong role:VaiTroHeThong.values()){
            var s=session(role);var db=new ShiftJdbcDouble();var service=new PhanCongService(s,db::open);boolean manager=role==VaiTroHeThong.SYSTEM_ADMIN||role==VaiTroHeThong.CLINIC_MANAGER;
            check(new NavigationPolicy().canOpen(s,Module.SHIFT_ASSIGNMENTS)==manager,"assignment role "+role);
            check(new NavigationPolicy().canOpen(s,Module.MY_SCHEDULE),"personal role "+role);
            if(manager){check(service.management(day,day,"",null).employees().size()==2,"employee choices");}
            else{rejects(AccessDeniedException.class,()->service.management(day,day,"",null));rejects(AccessDeniedException.class,()->service.create("NV1","CA1",day,"Nhiệm vụ"));rejects(AccessDeniedException.class,()->service.cancel("PC1","Lý do"));check(db.opens==0,"denied before JDBC");}
            db.assignments.add(ShiftJdbcDouble.assignment("OWN","NV1","CHO_HIEU_LUC"));db.assignments.add(ShiftJdbcDouble.assignment("OTHER","NV2","CHO_HIEU_LUC"));
            var mine=service.mine(day,day);check(mine.rows().size()==1&&mine.rows().getFirst().id().equals("OWN"),"cannot read another employee "+role);
            check("ACCOUNT1".equals(db.queriedAccount)&&"NV1".equals(db.queriedEmployee),"trusted association");
            db.linkedEmployee=null;check(service.mine(day,day).rows().isEmpty(),"missing association empty");check(!service.mine(day,day).message().isBlank(),"missing association feedback");
            s.logout();int opens=db.opens;rejects(AccessDeniedException.class,()->service.mine(day,day));rejects(AccessDeniedException.class,()->service.create("NV1","CA1",day,"task"));check(opens==db.opens,"logout before JDBC");
        }
        check(FeatureBootstrap.createRegistry().contains(Module.SHIFT_ASSIGNMENTS)&&FeatureBootstrap.createRegistry().contains(Module.MY_SCHEDULE),"both real factories registered");
        var db=new ShiftJdbcDouble();var s=session(VaiTroHeThong.CLINIC_MANAGER);var service=new PhanCongService(s,db::open);
        rejects(IllegalArgumentException.class,()->service.create("NV1","CA1",day," "));rejects(IllegalArgumentException.class,()->service.cancel("P1"," "));rejects(IllegalArgumentException.class,()->service.mine(day,day.minusDays(1)));check(db.opens==0,"validation before JDBC");
        db.database="N08Dental";rejects(SQLException.class,()->service.create("NV1","CA1",day,"task"));check(db.events.stream().noneMatch(e->e.startsWith("INSERT")),"real database blocked before writes");
        db.database="N08Dev_1234abcd";String id=service.create("NV1","CA1",day,"Hỗ trợ khám");check(id.length()==10&&db.commits==1,"create committed");check(db.assignments.size()==1,"read back");check(service.mine(day,day).rows().getFirst().nhiemVu().equals("Hỗ trợ khám"),"personal read back");
        check(db.events.stream().anyMatch(e->e.contains("UPDLOCK,HOLDLOCK")),"employee serialization lock");
        rejects(IllegalArgumentException.class,()->service.create("NV1","CA1",day,"duplicate"));check(db.assignments.size()==1,"overlap rejected");
        service.cancel(id,"Đổi kế hoạch");check(db.commits==2&&db.assignments.size()==1&&db.assignments.getFirst().get("trangThai").equals("DA_HUY"),"cancel preserves history");check(service.mine(day,day).rows().getFirst().lyDoHuy().equals("Đổi kế hoạch"),"reason read back");rejects(IllegalArgumentException.class,()->service.cancel(id,"again"));
        for(String failure:List.of("audit","commit","logout","missing employee","missing shift","inactive")){
            var d=new ShiftJdbcDouble();var principal=session(VaiTroHeThong.SYSTEM_ADMIN);var svc=new PhanCongService(principal,d::open);
            if(failure.equals("audit"))d.failAudit=true;if(failure.equals("commit"))d.failCommit=true;if(failure.equals("logout"))d.onAudit=principal::logout;
            if(failure.equals("missing employee"))d.missingEmployee=true;if(failure.equals("missing shift"))d.missingShift=true;if(failure.equals("inactive"))d.employeeStatus="NGHI_VIEC";
            rejects(failure.equals("logout")?AccessDeniedException.class:failure.startsWith("missing")||failure.equals("inactive")?IllegalArgumentException.class:SQLException.class,()->svc.create("NV1","CA1",day,"task"));
            check(d.commits==0&&d.assignments.isEmpty()&&d.rollbacks==1,"rollback "+failure);
        }
        var ca=new CaLamViec("C","Ca",LocalTime.of(8,0),LocalTime.of(12,0));var adjacent=new CaLamViec("B","Ca sau",LocalTime.of(12,0),LocalTime.of(16,0));
        var p=new ThongTinPhanCong("P","N","Tên",ca,day,"task","CHO_HIEU_LUC",null);PhanCongService.checkAssignment(adjacent,List.of(p),2);check(true,"adjacent permitted");
        rejects(IllegalArgumentException.class,()->PhanCongService.checkAssignment(adjacent,List.of(p),1));rejects(IllegalArgumentException.class,()->PhanCongService.checkAssignment(ca,List.of(p),2));
        var cancelled=new ThongTinPhanCong("P","N","Tên",ca,day,"task","DA_HUY","lý do");PhanCongService.checkAssignment(ca,List.of(cancelled),2);check(true,"cancelled excluded");
        rejects(IllegalArgumentException.class,()->PhanCongService.checkAssignment(new CaLamViec("N","Đêm",LocalTime.of(22,0),LocalTime.of(6,0)),List.of(),2));
        // PO APPROVED 01/11/2026 transition matrix: only CHO_HIEU_LUC may be cancelled.
        for(String state:List.of("CHO_HIEU_LUC","DANG_THUC_HIEN","HOAN_THANH","DA_HUY")){
            var d=new ShiftJdbcDouble();d.assignments.add(ShiftJdbcDouble.assignment("STATE1","NV1",state));
            var svc=new PhanCongService(session(VaiTroHeThong.CLINIC_MANAGER),d::open);
            boolean allowed=state.equals("CHO_HIEU_LUC");
            check(PhanCongService.canCancelStatus(state)==allowed,"cancel policy "+state);
            if(allowed){svc.cancel("STATE1","Lý do");check(d.commits==1&&d.assignments.getFirst().get("trangThai").equals("DA_HUY"),"allowed transition commits");}
            else{rejects(IllegalArgumentException.class,()->svc.cancel("STATE1","Lý do"));check(d.commits==0&&d.rollbacks==1&&d.assignments.getFirst().get("trangThai").equals(state),"forbidden state unchanged "+state);check(d.events.stream().noneMatch(e->e.startsWith("INSERT INTO dbo.NhatKyThaoTac")),"no success audit on denied transition");}
        }
        for(String advanced:List.of("DANG_THUC_HIEN","HOAN_THANH","DA_HUY")){
            var d=new ShiftJdbcDouble();d.assignments.add(ShiftJdbcDouble.assignment("RACE1","NV1","CHO_HIEU_LUC"));d.concurrentCancelState=advanced;
            var svc=new PhanCongService(session(VaiTroHeThong.SYSTEM_ADMIN),d::open);
            rejects(IllegalArgumentException.class,()->svc.cancel("RACE1","UI selected stale row"));
            check(d.commits==0&&d.rollbacks==1&&d.assignments.getFirst().get("trangThai").equals(advanced),"atomic UPDATE protects concurrent transition "+advanced);
            check(d.events.stream().noneMatch(e->e.startsWith("INSERT INTO dbo.NhatKyThaoTac")),"no cancellation audit on lost race");
        }
        var unexpected=new ShiftJdbcDouble();unexpected.assignments.add(ShiftJdbcDouble.assignment("MULTI","NV1","CHO_HIEU_LUC"));unexpected.cancelAffectedRows=2;
        var unexpectedService=new PhanCongService(session(VaiTroHeThong.SYSTEM_ADMIN),unexpected::open);
        rejects(IllegalArgumentException.class,()->unexpectedService.cancel("MULTI","Lý do"));
        check(unexpected.commits==0&&unexpected.rollbacks==1&&unexpected.assignments.getFirst().get("trangThai").equals("CHO_HIEU_LUC"),"unexpected affected-row count rolls back");
        check(unexpected.events.stream().noneMatch(e->e.startsWith("INSERT INTO dbo.NhatKyThaoTac")),"no audit for affected-row count other than one");
        check(!PhanCongService.canCancelStatus(null)&&!PhanCongService.canCancelStatus("UNKNOWN"),"unknown state denied");
        System.out.println("PASS: PhanCongSelfTest "+checks+" synthetic checks; no real SQL; transaction/identity/roles/overlap/history.");
    }
}
