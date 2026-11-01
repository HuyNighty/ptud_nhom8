package com.quanlynhakhoa.nghiepvu.dieutri.service;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import com.quanlynhakhoa.nghiepvu.dieutri.dao.TreatmentPlanDao;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public final class TreatmentPlanService {
    private final DesktopSession session;
    private final AuthorizationService authorization=new AuthorizationService(new RolePermissionPolicy());
    private final TreatmentPlanDao dao=new TreatmentPlanDao();

    public TreatmentPlanService(DesktopSession session){
        if(session==null)throw new IllegalArgumentException("Cần phiên đăng nhập.");
        this.session=session;
    }

    public List<Object[]> danhSach()throws SQLException{
        authorization.require(session,Permission.MEDICAL_RECORD_READ);
        return dao.danhSach();
    }

    public List<Object[]> chiTiet(String planId)throws SQLException{
        authorization.require(session,Permission.MEDICAL_RECORD_READ);
        return dao.chiTiet(code(planId,"Mã kế hoạch"));
    }

    public void taoKeHoach(String planId,String chartId,String visitId,LocalDate date,String goal)throws SQLException{
        authorization.require(session,Permission.MEDICAL_RECORD_UPDATE);
        String id=code(planId,"Mã kế hoạch"),chart=code(chartId,"Mã hồ sơ");
        String visit=visitId==null||visitId.isBlank()?null:code(visitId,"Mã phiếu khám");
        if(date==null)throw new IllegalArgumentException("Ngày lập kế hoạch bắt buộc.");
        required(goal,300,"Mục tiêu");
        tx(c->dao.taoKeHoach(c,session.accountId(),id,chart,visit,date,goal.trim()));
    }

    public void themBuoc(String planId,String serviceId,String tooth,LocalDate planned,String note)throws SQLException{
        authorization.require(session,Permission.MEDICAL_RECORD_UPDATE);
        String plan=code(planId,"Mã kế hoạch"),service=code(serviceId,"Mã dịch vụ");
        required(tooth,50,"Vị trí răng");if(planned==null)throw new IllegalArgumentException("Ngày dự kiến bắt buộc.");
        optional(note,300,"Ghi chú");tx(c->dao.themChiTiet(c,session.accountId(),plan,service,tooth.trim(),planned,clean(note)));
    }

    public void hoanThanhBuoc(int detailId,LocalDate actual,String result)throws SQLException{
        authorization.require(session,Permission.MEDICAL_RECORD_UPDATE);
        if(detailId<=0||actual==null)throw new IllegalArgumentException("Chi tiết hoặc ngày thực hiện không hợp lệ.");
        required(result,500,"Kết quả");
        tx(c->dao.hoanThanhBuoc(c,session.accountId(),detailId,actual,result.trim()));
    }

    public void huyBuoc(int detailId,String reason)throws SQLException{
        authorization.require(session,Permission.MEDICAL_RECORD_UPDATE);
        if(detailId<=0)throw new IllegalArgumentException("Chưa chọn bước điều trị.");
        required(reason,300,"Lý do hủy");tx(c->dao.huyBuoc(c,session.accountId(),detailId,reason.trim()));
    }

    public void hoanThanhKeHoach(String planId)throws SQLException{
        authorization.require(session,Permission.MEDICAL_RECORD_UPDATE);
        String id=code(planId,"Mã kế hoạch");
        tx(c->dao.hoanThanhKeHoach(c,session.accountId(),id));
    }

    public void huyKeHoach(String planId,String reason)throws SQLException{
        authorization.require(session,Permission.MEDICAL_RECORD_UPDATE);
        String id=code(planId,"Mã kế hoạch");
        required(reason,300,"Lý do hủy");
        tx(c->dao.huyKeHoach(c,session.accountId(),id,reason.trim()));
    }

    private static void tx(SqlAction action)throws SQLException{
        try(Connection c=TeamDevConnection.open()){c.setAutoCommit(false);
            try{action.run(c);
                c.commit();
            } catch(SQLException|RuntimeException e){
                try{c.rollback();
                } catch(SQLException rollback){
                    e.addSuppressed(rollback);
                }
                throw e;
            }
        }
    }

    private static String code(String value,String label){String s=value==null?"":value.trim().toUpperCase(Locale.ROOT);
        if(!s.matches("[A-Z0-9_-]{1,10}"))throw new IllegalArgumentException(label+" bắt buộc, tối đa 10 ký tự A-Z, 0-9, _ hoặc -.");
        return s;
    }

    private static void required(String s,int max,String label){
        if(s==null||s.trim().isEmpty()||s.trim().length()>max)throw new IllegalArgumentException(label+" bắt buộc và tối đa "+max+" ký tự.");
    }

    private static void optional(String s,int max,String label){
        if(s!=null&&s.trim().length()>max)throw new IllegalArgumentException(label+" tối đa "+max+" ký tự.");
    }

    private static String clean(String s){
        return s==null||s.isBlank()?null:s.trim();
    }

    @FunctionalInterface private interface SqlAction{
        void run(Connection c)throws SQLException;
    }
}
