package com.quanlynhakhoa.dao.dieutri;

import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** JDBC layer for V5 KeHoachDieuTri and ChiTietDieuTri. */
public final class KeHoachDieuTriDao {
    public List<Object[]> danhSach() throws SQLException {
        String sql="SELECT TOP (300) k.maKeHoach,k.maHoSo,b.maBenhNhan,b.hoTen,k.maPhieuKham,k.ngayLap,k.mucTieu,k.trangThai,nv.hoTen "
                +"FROM dbo.KeHoachDieuTri k INNER JOIN dbo.HoSoBenhAn h ON h.maHoSo=k.maHoSo "
                +"INNER JOIN dbo.BenhNhan b ON b.maBenhNhan=h.maBenhNhan INNER JOIN dbo.NhanVien nv ON nv.maNhanVien=k.maNhanVien "
                +"ORDER BY k.ngayLap DESC,k.maKeHoach DESC";
        return query(sql,9);
    }
    public List<Object[]> chiTiet(String planId)throws SQLException{
        String sql="SELECT d.maChiTiet,d.maDichVu,v.tenDichVu,d.viTriRang,d.ngayDuKien,d.ngayThucHien,d.ketQua,d.trangThai,nv.hoTen,d.ghiChu "
                +"FROM dbo.ChiTietDieuTri d INNER JOIN dbo.DichVu v ON v.maDichVu=d.maDichVu LEFT JOIN dbo.NhanVien nv ON nv.maNhanVien=d.maNhanVienThucHien "
                +"WHERE d.maKeHoach=? ORDER BY d.ngayDuKien,d.maChiTiet";
        List<Object[]> rows=new ArrayList<>();
        try(Connection con=TeamDevConnection.open();
            PreparedStatement stmt=con.prepareStatement(sql)){stmt.setString(1,planId);
            try(ResultSet rs=stmt.executeQuery()) {
                while (rs.next())
                    rows.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getNString(3), rs.getNString(4), rs.getDate(5), rs.getDate(6), rs.getNString(7), rs.getString(8), rs.getNString(9), rs.getNString(10)});
            }
        }
        return rows;
    }
    public void taoKeHoach(Connection con,String accountId,String planId,String chartId,String visitId,LocalDate date,String goal)throws SQLException{
        String employee=HoTroSqlKeHoachDieuTri.layMaNhanVien(con,accountId);
        if(visitId!=null){
            try(PreparedStatement stmt=con.prepareStatement("SELECT 1 FROM dbo.PhieuKham WITH (UPDLOCK,HOLDLOCK) WHERE maPhieuKham=? AND maHoSo=? AND trangThai<>'DA_HUY'")){
                stmt.setString(1,visitId);
                stmt.setString(2,chartId);
                try(ResultSet rs=stmt.executeQuery()){
                    if(!rs.next())throw new IllegalArgumentException("Phiếu khám không thuộc hồ sơ bệnh án đã chọn.");
                }
            }
        }
        if(visitId!=null)try(PreparedStatement guard=con.prepareStatement("SELECT maHoaDon FROM dbo.HoaDon WITH (UPDLOCK,HOLDLOCK) WHERE maPhieuKham=? AND trangThai<>'DA_HUY'")){
            guard.setQueryTimeout(15);guard.setString(1,visitId);
            try(ResultSet rows=guard.executeQuery()){if(rows.next())throw new IllegalArgumentException("Lượt khám đã chốt hóa đơn. Tạo lượt khám mới để bổ sung điều trị.");}
        }
        try(PreparedStatement stmt=con.prepareStatement("INSERT INTO dbo.KeHoachDieuTri(maKeHoach,maHoSo,maPhieuKham,ngayLap,mucTieu,trangThai,maNhanVien) VALUES (?,?,?,?,?,'DANG_THUC_HIEN',?)")){
            stmt.setString(1,planId);
            stmt.setString(2,chartId);
            stmt.setString(3,visitId);
            stmt.setDate(4,Date.valueOf(date));
            stmt.setNString(5,goal);
            stmt.setString(6,employee);
            stmt.executeUpdate();
        }
        HoTroSqlKeHoachDieuTri.ghiNhatKy(con,accountId,"Tạo kế hoạch điều trị","KeHoachDieuTri",planId);
    }
    public void themChiTiet(Connection con,String accountId,String planId,String serviceId,String tooth,LocalDate planned,String note)throws SQLException{
        String sql="INSERT INTO dbo.ChiTietDieuTri(maKeHoach,maDichVu,viTriRang,ngayDuKien,trangThai,ghiChu) "
                +"SELECT ?,?,?,?,'CHUA_THUC_HIEN',? WHERE EXISTS(SELECT 1 FROM dbo.KeHoachDieuTri WHERE maKeHoach=? AND trangThai='DANG_THUC_HIEN') "
                +"AND EXISTS(SELECT 1 FROM dbo.DichVu WHERE maDichVu=? AND dangApDung=1)";
        try(PreparedStatement stmt=con.prepareStatement(sql)){
            stmt.setString(1,planId);
            stmt.setString(2,serviceId);
            stmt.setNString(3,tooth);
            stmt.setDate(4,Date.valueOf(planned));
            stmt.setNString(5,note);
            stmt.setString(6,planId);
            stmt.setString(7,serviceId);
            if(stmt.executeUpdate()!=1)throw new IllegalArgumentException("Kế hoạch không ở trạng thái đang thực hiện hoặc dịch vụ không còn áp dụng.");
        }
        HoTroSqlKeHoachDieuTri.ghiNhatKy(con,accountId,"Thêm bước điều trị","ChiTietDieuTri",planId);
    }
    public void hoanThanhBuoc(Connection con,String accountId,int detailId,LocalDate actual,String result)throws SQLException{
        String employee=HoTroSqlKeHoachDieuTri.layMaNhanVien(con,accountId);
        String sql="UPDATE d SET d.trangThai='DA_THUC_HIEN',d.ngayThucHien=?,d.ketQua=?,d.maNhanVienThucHien=? FROM dbo.ChiTietDieuTri d "
                +"INNER JOIN dbo.KeHoachDieuTri k ON k.maKeHoach=d.maKeHoach WHERE d.maChiTiet=? AND d.trangThai='CHUA_THUC_HIEN' AND k.trangThai='DANG_THUC_HIEN'";
        try(PreparedStatement stmt=con.prepareStatement(sql)){
            stmt.setDate(1,Date.valueOf(actual));
            stmt.setNString(2,result);
            stmt.setString(3,employee);
            stmt.setInt(4,detailId);
            if(stmt.executeUpdate()!=1)throw new IllegalArgumentException("Bước điều trị không còn ở trạng thái chờ thực hiện.");
        }
        HoTroSqlKeHoachDieuTri.ghiNhatKy(con,accountId,"Hoàn thành bước điều trị","ChiTietDieuTri",String.valueOf(detailId));
    }
    public void huyBuoc(Connection con,String accountId,int detailId,String reason)throws SQLException{
        String sql="UPDATE d SET d.trangThai='DA_HUY',d.ghiChu=? FROM dbo.ChiTietDieuTri d INNER JOIN dbo.KeHoachDieuTri k ON k.maKeHoach=d.maKeHoach "
                +"WHERE d.maChiTiet=? AND d.trangThai='CHUA_THUC_HIEN' AND k.trangThai='DANG_THUC_HIEN'";
        try(PreparedStatement stmt=con.prepareStatement(sql)){
            stmt.setNString(1,reason);
            stmt.setInt(2,detailId);
            if(stmt.executeUpdate()!=1)throw new IllegalArgumentException("Bước điều trị không còn ở trạng thái chờ thực hiện.");
        }
        HoTroSqlKeHoachDieuTri.ghiNhatKy(con,accountId,"Hủy bước điều trị","ChiTietDieuTri",String.valueOf(detailId));
    }
    public void hoanThanhKeHoach(Connection con,String accountId,String planId)throws SQLException{
        String sql="UPDATE k SET trangThai='HOAN_THANH' FROM dbo.KeHoachDieuTri k WHERE k.maKeHoach=? AND k.trangThai='DANG_THUC_HIEN' "
                +"AND EXISTS(SELECT 1 FROM dbo.ChiTietDieuTri d WHERE d.maKeHoach=k.maKeHoach AND d.trangThai='DA_THUC_HIEN') "
                +"AND NOT EXISTS(SELECT 1 FROM dbo.ChiTietDieuTri d WHERE d.maKeHoach=k.maKeHoach AND d.trangThai='CHUA_THUC_HIEN')";
        try(PreparedStatement stmt=con.prepareStatement(sql)){
            stmt.setString(1,planId);
            if(stmt.executeUpdate()!=1)throw new IllegalArgumentException("Cần ít nhất một bước đã hoàn thành và không còn bước chờ trước khi hoàn tất kế hoạch.");
        }
        HoTroSqlKeHoachDieuTri.ghiNhatKy(con,accountId,"Hoàn tất kế hoạch điều trị","KeHoachDieuTri",planId);
    }
    public void huyKeHoach(Connection con,String accountId,String planId,String reason)throws SQLException{
        try(PreparedStatement stmt=con.prepareStatement("UPDATE dbo.KeHoachDieuTri SET trangThai='DA_HUY',lyDoHuy=? WHERE maKeHoach=? AND trangThai='DANG_THUC_HIEN'")){
            stmt.setNString(1,reason);
            stmt.setString(2,planId);
            if(stmt.executeUpdate()!=1)throw new IllegalArgumentException("Chỉ có thể hủy kế hoạch đang thực hiện.");
        }
        HoTroSqlKeHoachDieuTri.ghiNhatKy(con,accountId,"Hủy kế hoạch điều trị","KeHoachDieuTri",planId);
    }
    private List<Object[]> query(String sql,int width)throws SQLException{List<Object[]> rows=new ArrayList<>();
        try(Connection con=TeamDevConnection.open();
            PreparedStatement stmt=con.prepareStatement(sql);
            ResultSet rs=stmt.executeQuery()){
            while(rs.next()){
                Object[] row=new Object[width];
                for(int i=0;i<width;i++)
                    row[i]=rs.getObject(i+1);
                rows.add(row);
            }
        }
        return rows;
    }
}
