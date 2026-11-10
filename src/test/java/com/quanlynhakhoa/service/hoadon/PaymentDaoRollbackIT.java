package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.entity.ThanhToan;

import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.dao.hoadon.*;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.model.hoadon.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Opt-in DAO protocol acceptance on existing synthetic fixtures; every write is rolled back. */
public final class PaymentDaoRollbackIT {
    private static int checks;
    private static void check(boolean ok,String why) { checks++; if(!ok)throw new AssertionError(why); }
    private static String key() { return "IT"+UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase(Locale.ROOT); }
    private static long count(Connection c,String sql,String key)throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)) {
            s.setQueryTimeout(10);s.setString(1,key);
            try(ResultSet r=s.executeQuery()){r.next();return r.getLong(1);}
        }
    }
    private static String one(Connection c,String sql)throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)) {
            s.setQueryTimeout(10);
            try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Required synthetic SQL fixture is missing");return r.getString(1);}
        }
    }
    private static YeuCauThanhToan request(String invoice,String id) {
        return new YeuCauThanhToan(invoice,BigDecimal.ONE,List.of(
                new PhanThanhToan(id,BigDecimal.ONE,PhuongThucThanhToan.TIEN_MAT,null,new BigDecimal("2"))),false);
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1||!args[0].equals("--rollback-check")||!TeamDevConnection.hasLocalConfig())
            throw new IllegalArgumentException("Requires approved isolated local DB and --rollback-check");
        String first=key(),second=key(),payment;ThanhToan before;long auditBefore;
        String auditSql="SELECT COUNT(*) FROM dbo.NhatKyThaoTac WHERE doiTuong='ThanhToan' AND maDoiTuong=?";
        String attemptSql="SELECT COUNT(*) FROM dbo.LanThuHoaDon WHERE maLanThu=?";
        String adjustmentSql="SELECT COUNT(*) FROM dbo.DieuChinhThanhToan WHERE maThanhToan=?";
        try(Connection c=TeamDevConnection.open()) {
            String account;
            try(PreparedStatement s=c.prepareStatement("SELECT maTaiKhoan FROM dbo.TaiKhoan WHERE tenDangNhap=? AND trangThai='HOAT_DONG' AND maNhanVien IS NOT NULL")) {
                s.setQueryTimeout(10);s.setString(1,"dev.admin");
                try(ResultSet r=s.executeQuery()){check(r.next(),"active linked test account");account=r.getString(1);check(!r.next(),"unique test account");}
            }
            String invoice=one(c,"SELECT TOP(1) h.maHoaDon FROM dbo.HoaDon h WHERE (h.maHoaDon LIKE 'TST%' OR h.maHoaDon LIKE 'QA%') AND h.trangThai IN ('CHUA_THANH_TOAN','THANH_TOAN_MOT_PHAN') AND NOT EXISTS(SELECT 1 FROM dbo.LanThuHoaDon l WHERE l.maHoaDon=h.maHoaDon AND l.trangThai='CHO_XU_LY') ORDER BY h.maHoaDon");
            payment=one(c,"SELECT TOP(1) t.maThanhToan FROM dbo.ThanhToan t JOIN dbo.HoaDon h ON h.maHoaDon=t.maHoaDon WHERE (t.maThanhToan LIKE 'TST%' OR t.maThanhToan LIKE 'QA%') AND t.trangThai='THANH_CONG' AND h.trangThai<>'DA_HUY' AND NOT EXISTS(SELECT 1 FROM dbo.DieuChinhThanhToan d WHERE d.maThanhToan=t.maThanhToan) ORDER BY t.maThanhToan");
            before=new ThanhToanDao().existing(c,payment);auditBefore=count(c,auditSql,payment);
            check(count(c,attemptSql,first)==0&&count(c,attemptSql,second)==0,"fresh test attempt keys");
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try {
                LanThanhToanDao attempts=new LanThanhToanDao();attempts.khoaHoaDon(c,invoice);
                check(attempts.pending(c,invoice)==null,"fixture has no pending reservation");
                var request=request(invoice,first);attempts.taoLanThu(c,request,account,"CHO_XU_LY");
                var restored=attempts.pending(c,invoice);
                check(restored!=null&&restored.request().equals(request)&&restored.owner().equals(account),"insert and pending mapping");
                boolean[] visited={false};
                check(attempts.voiLanThuDaKhoa(c,first,new LanThanhToanDao.XuLyLanThu(){
                    public void xuLy(ThongTinLanThuDaKhoa row) {
                        visited[0]=true;check(row.maHoaDon().equals(invoice)&&row.maTaiKhoan().equals(account),"locked owner/invoice");
                        check(row.soTien().compareTo(BigDecimal.ONE)==0&&row.phuongThuc().equals("TIEN_MAT")&&row.trangThai().equals("CHO_XU_LY"),"locked amount/method/status");
                    }
                })&&visited[0],"locked callback executed");
                attempts.kiemTraLanThuConHieuLuc(c,request,account);
                attempts.xacNhanHoanTat(c,request,account);check(attempts.pending(c,invoice)==null,"ack removes pending row");
                attempts.taoLanThu(c,request(invoice,second),account,"CHO_XU_LY");
                attempts.ketThucDoiSoat(c,invoice,second,"DAO rollback acceptance",account,"DA_HUY");
                attempts.ghiNhatKyDoiSoat(c,account,second);check(attempts.pending(c,invoice)==null,"release removes pending row");
                DaoThanhToanDao reversals=new DaoThanhToanDao();reversals.khoaHoaDon(c,before.maHoaDon());
                Timestamp when=reversals.thoiGianHienTai(c);check(when!=null,"server timestamp");
                reversals.taoDieuChinh(c,payment,"HOAN_TIEN",before.soTien(),when,account,"DAO rollback acceptance","N08_DAO_ROLLBACK");
                reversals.capNhatKhoanThu(c,payment,"DA_HOAN_TIEN",when,account,"DAO rollback acceptance");
                reversals.ghiNhatKy(c,payment,account,"Hoàn tiền");
                check(count(c,adjustmentSql,payment)==1,"adjustment read back in transaction");
                check(new ThanhToanDao().existing(c,payment).trangThai().name().equals("DA_HOAN_TIEN"),"payment update mapped");
            } finally { c.rollback(); }
        }
        try(Connection c=TeamDevConnection.open()) {
            check(count(c,attemptSql,first)==0&&count(c,attemptSql,second)==0,"both synthetic attempts fully rolled back");
            check(count(c,"SELECT COUNT(*) FROM dbo.NhatKyThaoTac WHERE doiTuong='LanThuHoaDon' AND maDoiTuong=?",second)==0,"synthetic reservation audit rolled back");
            check(count(c,adjustmentSql,payment)==0,"adjustment rolled back");
            check(before.equals(new ThanhToanDao().existing(c,payment)),"original payment restored exactly");
            check(count(c,auditSql,payment)==auditBefore,"original payment audit count restored");
        }
        System.out.println("PASS PaymentDaoRollbackIT: "+checks+" actual SQL checks; reservation/ack/release/refund mapping and audit verified; all writes rolled back.");
    }
}
