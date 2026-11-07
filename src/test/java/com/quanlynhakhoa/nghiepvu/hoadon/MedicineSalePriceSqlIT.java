package com.quanlynhakhoa.nghiepvu.hoadon;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.*;
import com.quanlynhakhoa.nghiepvu.hoadon.service.JdbcClinicalBillingSource;
import java.math.BigDecimal;
import java.sql.*;
import static com.quanlynhakhoa.nghiepvu.hoadon.ServiceBillingSelfTest.*;

/** Opt-in synthetic local fixture. All writes, including price changes, are rolled back. */
public final class MedicineSalePriceSqlIT {
    public static void main(String[] args) throws Exception {
        if(args.length!=1||!args[0].equals("--rollback-check")||!TeamDevConnection.hasLocalConfig())
            throw new IllegalArgumentException("Use local --rollback-check with TST fixture data");
        BigDecimal oldPrice;
        try(Connection c=TeamDevConnection.open()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try(Statement s=c.createStatement()){
                try(ResultSet r=s.executeQuery("SELECT COUNT(*) FROM dbo.PhieuKham WHERE maPhieuKham='TSTPRPK01'")){r.next();check(r.getInt(1)==0,"test visit id must be unused");}
                try(ResultSet r=s.executeQuery("SELECT donGiaBan FROM dbo.Thuoc WHERE maThuoc='TSTTH01'")){check(r.next(),"requires synthetic medicine TSTTH01");oldPrice=r.getBigDecimal(1);}
                s.executeUpdate("INSERT INTO dbo.PhieuKham(maPhieuKham,maBenhNhan,maHoSo,maNhanVien,thoiGianTiepNhan,chanDoan,trangThai) VALUES('TSTPRPK01','TSTBN01','TSTHS01','TSTNV01',CURRENT_TIMESTAMP,N'[KIỂM THỬ HOÀN TÁC]','DA_KET_LUAN')");
                s.executeUpdate("INSERT INTO dbo.DonThuoc(maDonThuoc,maPhieuKham,maBenhNhan,maNhanVien,ngayKe,loiDan) VALUES('TSTPRDT01','TSTPRPK01','TSTBN01','TSTNV01',CAST(GETDATE() AS date),N'[KIỂM THỬ HOÀN TÁC]')");
                s.executeUpdate("INSERT INTO dbo.ChiTietDonThuoc(maDonThuoc,maThuoc,soLuong,lieuDung,cachDung,soNgayDung) VALUES('TSTPRDT01','TSTTH01',3,N'Mô phỏng',N'Mô phỏng',1)");
                s.executeUpdate("UPDATE dbo.Thuoc SET donGiaBan=12500 WHERE maThuoc='TSTTH01'");
                var source=new JdbcClinicalBillingSource();
                var draft=source.load(c,"TSTBN01","TSTPRPK01",true);
                check(draft.lines().size()==1&&draft.lines().getFirst().medicine(),"medicine-only invoice source");
                check(draft.total().compareTo(new BigDecimal("37500"))==0,"prescribed quantity times sale price");
                InvoiceCreateDao writer=new InvoiceCreateDao();writer.requireWritePermissions(c);
                writer.validateVisit(c,"TSTBN01","TSTPRPK01");
                writer.insertHeader(c,"TSTPRHD01","TSTBN01","TSTPRPK01",writer.employee(c,"TSTTK02"));
                for(var line:draft.lines())writer.insertLine(c,"TSTPRHD01",line);
                writer.audit(c,"TSTTK02","TSTPRHD01");
                s.executeUpdate("UPDATE dbo.Thuoc SET donGiaBan=15000 WHERE maThuoc='TSTTH01'");
                var changed=source.load(c,"TSTBN01","TSTPRPK01",true);
                check(!draft.equals(changed),"price change invalidates prior preview");
                var saved=new InvoiceDao().lines(c,"TSTPRHD01");
                check(saved.size()==1&&saved.getFirst().maThuoc().equals("TSTTH01")
                        &&saved.getFirst().donGiaApDung().compareTo(new BigDecimal("12500"))==0,"saved price unaffected by catalogue changes");
                check(saved.getFirst().thanhTien().compareTo(new BigDecimal("37500"))==0,"invoice amount remains locked");
                s.executeUpdate("UPDATE dbo.Thuoc SET donGiaBan=NULL WHERE maThuoc='TSTTH01'");
                expect(IllegalArgumentException.class,()->source.load(c,"TSTBN01","TSTPRPK01",true));
            }finally{c.rollback();}
        }
        try(Connection c=TeamDevConnection.open();Statement s=c.createStatement()){
            try(ResultSet r=s.executeQuery("SELECT COUNT(*) FROM dbo.HoaDon WHERE maHoaDon='TSTPRHD01'")){r.next();check(r.getInt(1)==0,"test invoice rolled back");}
            try(ResultSet r=s.executeQuery("SELECT donGiaBan FROM dbo.Thuoc WHERE maThuoc='TSTTH01'")){r.next();check(java.util.Objects.equals(oldPrice,r.getBigDecimal(1)),"original catalogue price restored");}
        }
        System.out.println("PASS: SQL medicine sale price, prescribed quantities, locked invoice prices, changed-preview detection and missing-price rejection; all writes rolled back.");
    }
}
