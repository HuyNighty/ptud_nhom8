package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.billing.dao.*;
import com.quanlynhakhoa.billing.ui.BillingUi;
import com.quanlynhakhoa.dev.TeamDevConnection;
import java.sql.*;
import java.util.List;

/** Opt-in read-only diagnostics with the configured application SQL principal, no fabricated session. */
public final class BillingUiSqlPreflight {
    public static void main(String[] args)throws Exception {
        if(args.length!=1||!args[0].equals("--read-only")||!TeamDevConnection.hasLocalConfig())throw new IllegalArgumentException("Requires --read-only and existing approved dev config");
        try(Connection c=TeamDevConnection.open()){
            for(String table:List.of("HoaDon","ChiTietHoaDon","ThanhToan","YeuCauHuyHoaDon","DichVu","Thuoc","VatTu","PhieuKham","BenhNhan","ChiTietDieuTri","ChiDinhCanLamSang")){
                try(PreparedStatement s=c.prepareStatement("SELECT HAS_PERMS_BY_NAME(?, 'OBJECT', 'SELECT'), HAS_PERMS_BY_NAME(?, 'OBJECT', 'INSERT'), HAS_PERMS_BY_NAME(?, 'OBJECT', 'UPDATE')")){
                    s.setQueryTimeout(10);for(int i=1;i<=3;i++)s.setString(i,"dbo."+table);
                    try(ResultSet r=s.executeQuery()){r.next();System.out.println(table+": SELECT="+r.getInt(1)+" INSERT="+r.getInt(2)+" UPDATE="+r.getInt(3));}
                }
            }
            // No rows/data printed, only verified columns and query validity.
            try(PreparedStatement s=c.prepareStatement("SELECT TOP (0) d.tenDichVu,t.tenThuoc,v.tenVatTu,b.soDienThoai,p.thoiGianTiepNhan FROM dbo.DichVu d CROSS JOIN dbo.Thuoc t CROSS JOIN dbo.VatTu v CROSS JOIN dbo.BenhNhan b CROSS JOIN dbo.PhieuKham p")){
                s.setQueryTimeout(10);try(ResultSet ignored=s.executeQuery()){System.out.println("PASS: actual catalog-name, patient-phone and visit-time columns readable");}
            }
            new HoaDonDao().lines(c,"__UI_CHECK");System.out.println("PASS: detail-name joins execute on actual development schema (no invoice data printed)");
            try(PreparedStatement s=c.prepareStatement("SELECT TOP (0) maYeuCau,maHoaDon,trangThai FROM dbo.YeuCauHuyHoaDon")){
                s.setQueryTimeout(10);try(ResultSet ignored=s.executeQuery()){System.out.println("PASS: cancellation requests readable");}
            }catch(SQLException failure){System.out.println("BLOCKED: cancellation read SQLState="+failure.getSQLState()+" code="+failure.getErrorCode());System.out.println("UI: "+BillingUi.error(failure));if(failure.getErrorCode()!=229)throw failure;}
        }
        System.out.println("READ-ONLY COMPLETE: no GRANT, DDL, INSERT, UPDATE, DELETE or credential output.");
    }
}
