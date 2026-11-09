package com.quanlynhakhoa.quantri.lichlamviec.dao;

import com.quanlynhakhoa.model.lichlamviec.ThongTinPhanCong;
import com.quanlynhakhoa.model.lichlamviec.NhanVienChon;
import com.quanlynhakhoa.entity.CaLamViec;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** SQL contract: V1 account association, V4 audit, V5 shifts/assignments. No schema writes. */
public final class PhanCongDao {
    private PreparedStatement query(Connection c, String sql) throws SQLException {
        PreparedStatement p = c.prepareStatement(sql); p.setQueryTimeout(12); return p;
    }
    public List<NhanVienChon> employees(Connection c, String keyword) throws SQLException {
        try (var p = query(c, "SELECT maNhanVien,hoTen FROM dbo.NhanVien WHERE trangThai='DANG_LAM_VIEC' AND (? = N'' OR CHARINDEX(?,hoTen)>0 OR CHARINDEX(?,maNhanVien)>0) ORDER BY hoTen,maNhanVien")) {
            p.setNString(1,keyword); p.setNString(2,keyword); p.setNString(3,keyword);
            try(var r=p.executeQuery()) { List<NhanVienChon> rows=new ArrayList<>();
                while(r.next()) rows.add(new NhanVienChon(r.getString(1),r.getNString(2))); return List.copyOf(rows); }
        }
    }
    public List<CaLamViec> shifts(Connection c) throws SQLException {
        try(var p=query(c,"SELECT maCa,tenCa,gioBatDau,gioKetThuc FROM dbo.CaLamViec ORDER BY gioBatDau,maCa"); var r=p.executeQuery()) {
            List<CaLamViec> rows=new ArrayList<>(); while(r.next()) rows.add(shift(r)); return List.copyOf(rows);
        }
    }
    private CaLamViec shift(ResultSet r) throws SQLException {
        return new CaLamViec(r.getString("maCa"),r.getNString("tenCa"),r.getTime("gioBatDau").toLocalTime(),r.getTime("gioKetThuc").toLocalTime());
    }
    public List<ThongTinPhanCong> schedule(Connection c, LocalDate from, LocalDate to, String employee) throws SQLException {
        try(var p=query(c,"SELECT p.maPhanCong,p.maNhanVien,n.hoTen,p.ngayLam,p.nhiemVu,p.trangThai,p.lyDoHuy,c.maCa,c.tenCa,c.gioBatDau,c.gioKetThuc FROM dbo.PhanCong p JOIN dbo.NhanVien n ON n.maNhanVien=p.maNhanVien JOIN dbo.CaLamViec c ON c.maCa=p.maCa WHERE p.ngayLam>=? AND p.ngayLam<=? AND (? IS NULL OR p.maNhanVien=?) ORDER BY p.ngayLam,c.gioBatDau,n.hoTen,p.maPhanCong")) {
            p.setDate(1,java.sql.Date.valueOf(from)); p.setDate(2,java.sql.Date.valueOf(to)); p.setString(3,employee);p.setString(4,employee);
            try(var r=p.executeQuery()){List<ThongTinPhanCong> rows=new ArrayList<>();while(r.next())rows.add(new ThongTinPhanCong(r.getString("maPhanCong"),r.getString("maNhanVien"),r.getNString("hoTen"),shift(r),r.getDate("ngayLam").toLocalDate(),r.getNString("nhiemVu"),r.getString("trangThai"),r.getNString("lyDoHuy")));return List.copyOf(rows);}
        }
    }
    public String employeeForAccount(Connection c, String account) throws SQLException {
        try(var p=query(c,"SELECT n.maNhanVien FROM dbo.TaiKhoan a JOIN dbo.NhanVien n ON n.maNhanVien=a.maNhanVien WHERE a.maTaiKhoan=?")) {
            p.setString(1,account);try(var r=p.executeQuery()){return r.next()?r.getString(1):null;}
        }
    }
    /** Serializes every create/cancel for one employee, even when no assignments exist yet. */
    public void lockEmployee(Connection c,String id,LocalDate day,boolean requireActive) throws SQLException {
        try(var p=query(c,"SELECT trangThai,ngayVaoLam FROM dbo.NhanVien WITH (UPDLOCK,HOLDLOCK) WHERE maNhanVien=?")){
            p.setString(1,id);try(var r=p.executeQuery()){
                if(!r.next())throw new IllegalArgumentException("Nhân viên không còn tồn tại.");
                if(requireActive && (!"DANG_LAM_VIEC".equals(r.getString(1)) || (r.getDate(2)!=null && day.isBefore(r.getDate(2).toLocalDate()))))throw new IllegalArgumentException("Nhân viên không làm việc vào ngày đã chọn.");
            }
        }
    }
    public CaLamViec lockedShift(Connection c,String id) throws SQLException {
        try(var p=query(c,"SELECT maCa,tenCa,gioBatDau,gioKetThuc FROM dbo.CaLamViec WITH (HOLDLOCK) WHERE maCa=?")){
            p.setString(1,id);try(var r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Ca làm việc không còn tồn tại.");return shift(r);}
        }
    }
    public int maximumPerDay(Connection c) throws SQLException {
        try(var p=query(c,"SELECT giaTri FROM dbo.ThamSoHeThong WITH (HOLDLOCK) WHERE ten='SO_CA_TOI_DA_NGAY'");var r=p.executeQuery()){
            if(!r.next())return 2; // Documented default, THIET_KE_DU_LIEU section 9.2.
            try {int max=Integer.parseInt(r.getString(1));if(max<1)throw new NumberFormatException();return max;}
            catch(NumberFormatException e){throw new SQLException("Tham số số ca tối đa/ngày không hợp lệ.",e);}
        }
    }
    public void requireDevDatabase(Connection c) throws SQLException {
        try(var p=query(c,"SELECT DB_NAME()");var r=p.executeQuery()){
            if(!r.next() || r.getString(1)==null || !r.getString(1).matches("N08Dev_[0-9a-f]{8}"))
                throw new SQLException("BLOCKED: chỉ ghi vào database N08Dev_<8 hex> cô lập.");
        }
    }
    public void insertShift(Connection c, CaLamViec shift) throws SQLException {
        try (var p = query(c, "INSERT INTO dbo.CaLamViec(maCa,tenCa,gioBatDau,gioKetThuc) VALUES(?,?,?,?)")) {
            p.setString(1, shift.id()); p.setNString(2, shift.ten());
            p.setTime(3, Time.valueOf(shift.batDau())); p.setTime(4, Time.valueOf(shift.ketThuc()));
            one(p.executeUpdate());
        }
    }
    public void insert(Connection c,String id,String employee,String shift,LocalDate date,String task) throws SQLException {
        try(var p=query(c,"INSERT INTO dbo.PhanCong(maPhanCong,maNhanVien,maCa,ngayLam,nhiemVu,trangThai) VALUES(?,?,?,?,?,'CHO_HIEU_LUC')")){
            p.setString(1,id);p.setString(2,employee);p.setString(3,shift);p.setDate(4,java.sql.Date.valueOf(date));p.setNString(5,task);one(p.executeUpdate());
        }
    }
    public String assignmentEmployee(Connection c,String id) throws SQLException {
        try(var p=query(c,"SELECT maNhanVien FROM dbo.PhanCong WHERE maPhanCong=?")){
            p.setString(1,id);try(var r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Phân công không còn tồn tại.");return r.getString(1);}
        }
    }
    // PO APPROVED 01/11/2026: source-state check is atomic with the UPDATE, never a SELECT-then-write gate.
    public void cancel(Connection c,String id,String reason) throws SQLException {
        try(var p=query(c,"UPDATE dbo.PhanCong SET trangThai='DA_HUY',lyDoHuy=? WHERE maPhanCong=? AND trangThai='CHO_HIEU_LUC'")){
            p.setNString(1,reason);p.setString(2,id);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Chỉ hủy phân công chờ hiệu lực. Trạng thái có thể đã thay đổi; hãy tải lại.");
        }
    }
    public void audit(Connection c,String account,String id,String action) throws SQLException {
        try(var p=query(c,"INSERT INTO dbo.NhatKyThaoTac(loaiTacNhan,maTaiKhoan,chucNang,hanhDong,ketQua,doiTuong,maDoiTuong,noiDung) VALUES('ACCOUNT',?,N'PhanCong',?,'SUCCESS','PhanCong',?,N'Phân công ca làm việc')")){
            p.setString(1,account);p.setNString(2,action);p.setString(3,id);one(p.executeUpdate());
        }
    }
    private void one(int count) throws SQLException {if(count!=1)throw new SQLException("Số dòng ghi không hợp lệ.");}
}
