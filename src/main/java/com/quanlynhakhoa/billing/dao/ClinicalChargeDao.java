package com.quanlynhakhoa.billing.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Reads existing clinical evidence only; no invented visits or performed services. */
public class ClinicalChargeDao {
    public record Evidence(String kind, String sourceId, String serviceId, String name,
                           BigDecimal price, boolean active, boolean clinicalService) { }

    public List<Evidence> load(Connection c, String patient, String visit) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("""
                SELECT p.trangThai FROM dbo.PhieuKham p WITH (HOLDLOCK)
                JOIN dbo.HoSoBenhAn h WITH (HOLDLOCK) ON h.maHoSo=p.maHoSo
                WHERE p.maPhieuKham=? AND p.maBenhNhan=? AND h.maBenhNhan=?
                """)) {
            s.setQueryTimeout(15); s.setString(1,visit); s.setString(2,patient); s.setString(3,patient);
            try(ResultSet r=s.executeQuery()) {
                if(!r.next() || !"DA_KET_LUAN".equals(r.getString(1)))
                    throw new IllegalArgumentException("Chỉ lập hóa đơn khi lượt khám đã kết luận và đúng hồ sơ bệnh nhân.");
            }
        }
        // A single invoice per visit cannot safely support subsequent additions to an open plan.
        try (PreparedStatement s=c.prepareStatement("""
                SELECT k.maKeHoach FROM dbo.KeHoachDieuTri k WITH (HOLDLOCK)
                JOIN dbo.HoSoBenhAn h WITH (HOLDLOCK) ON h.maHoSo=k.maHoSo
                WHERE k.maPhieuKham=? AND (h.maBenhNhan<>? OR k.trangThai='DANG_THUC_HIEN'
                OR (k.trangThai='DA_HUY' AND EXISTS (SELECT 1 FROM dbo.ChiTietDieuTri d WITH (HOLDLOCK)
                    WHERE d.maKeHoach=k.maKeHoach AND d.trangThai='DA_THUC_HIEN'))
                OR EXISTS (SELECT 1 FROM dbo.ChiTietDieuTri d WITH (HOLDLOCK)
                    WHERE d.maKeHoach=k.maKeHoach AND d.trangThai='CHUA_THUC_HIEN'))
                """)) {
            s.setQueryTimeout(15);s.setString(1,visit);s.setString(2,patient);
            try(ResultSet r=s.executeQuery()){if(r.next())throw new IllegalArgumentException("Kế hoạch chưa hoàn tất hoặc cần đối soát hạng mục hủy/đã thực hiện.");}
        }
        List<Evidence> result=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement("""
                SELECT x.maChiDinh,x.trangThai,x.ketQua,x.thoiGianKetQua,
                       d.maDichVu,d.tenDichVu,d.donGia,d.dangApDung,d.laCanLamSang
                FROM dbo.ChiDinhCanLamSang x WITH (HOLDLOCK)
                JOIN dbo.DichVu d WITH (HOLDLOCK) ON d.maDichVu=x.maDichVu
                WHERE x.maPhieuKham=? ORDER BY x.maChiDinh
                """)) {
            s.setQueryTimeout(15);s.setString(1,visit);
            try(ResultSet r=s.executeQuery()) {while(r.next()) {
                if(!"DA_CO_KET_QUA".equals(r.getString("trangThai")) || r.getNString("ketQua")==null || r.getTimestamp("thoiGianKetQua")==null)
                    throw new IllegalArgumentException("Còn chỉ định CLS chưa có kết quả đầy đủ.");
                result.add(map(r,"CLS",r.getString("maChiDinh")));
            }}
        }
        try(PreparedStatement s=c.prepareStatement("""
                SELECT x.maChiTiet,x.ngayThucHien,x.ketQua,x.maNhanVienThucHien,
                       d.maDichVu,d.tenDichVu,d.donGia,d.dangApDung,d.laCanLamSang
                FROM dbo.ChiTietDieuTri x WITH (HOLDLOCK)
                JOIN dbo.KeHoachDieuTri k WITH (HOLDLOCK) ON k.maKeHoach=x.maKeHoach
                JOIN dbo.DichVu d WITH (HOLDLOCK) ON d.maDichVu=x.maDichVu
                WHERE k.maPhieuKham=? AND k.trangThai='HOAN_THANH' AND x.trangThai='DA_THUC_HIEN'
                ORDER BY x.maChiTiet
                """)) {
            s.setQueryTimeout(15);s.setString(1,visit);
            try(ResultSet r=s.executeQuery()){while(r.next()) {
                if(r.getDate("ngayThucHien")==null || r.getNString("ketQua")==null || r.getString("maNhanVienThucHien")==null)
                    throw new IllegalArgumentException("Hạng mục điều trị thiếu bằng chứng thực hiện.");
                result.add(map(r,"DIEU_TRI",r.getString("maChiTiet")));
            }}
        }
        return List.copyOf(result);
    }
    private static Evidence map(ResultSet r,String kind,String id)throws SQLException {
        return new Evidence(kind,id,r.getString("maDichVu"),r.getNString("tenDichVu"),r.getBigDecimal("donGia"),r.getBoolean("dangApDung"),r.getBoolean("laCanLamSang"));
    }
}
