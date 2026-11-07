package com.quanlynhakhoa.hethong.phanquyen;

import java.sql.*;
import com.quanlynhakhoa.hethong.xacthuc.JdbcAuthService.ConnectionSource;

/** Current account state is checked on each authorized operation, never just at login. */
public final class JdbcSessionVerifier implements DesktopSession.CurrentAccountCheck {
    private final ConnectionSource connections;
    public JdbcSessionVerifier(ConnectionSource connections){this.connections=connections;}
    @Override public void check(DesktopSession session){
        try(Connection c=connections.open()){verify(c,session,false);}
        catch(SQLException e){throw new IllegalStateException("Không xác minh được phiên đăng nhập. Kiểm tra kết nối trước khi thao tác.",e);}
    }
    public static void verify(Connection c,DesktopSession session,boolean lock)throws SQLException {
        String hint=lock?" WITH (HOLDLOCK)":"";
        try(PreparedStatement s=c.prepareStatement("SELECT a.vaiTro,a.trangThai,a.phaiDoiMatKhau,n.trangThai AS employeeStatus FROM dbo.TaiKhoan a"+hint+" LEFT JOIN dbo.NhanVien n"+hint+" ON n.maNhanVien=a.maNhanVien WHERE a.maTaiKhoan=?")){
            s.setQueryTimeout(8);s.setString(1,session.accountId());
            try(ResultSet rows=s.executeQuery()){
                if(!rows.next()||!session.role().name().equals(rows.getString(1))||!"HOAT_DONG".equals(rows.getString(2))||rows.getBoolean(3)
                   ||rows.getString(4)!=null&&!"DANG_LAM_VIEC".equals(rows.getString(4))){
                    session.logout();throw new AccessDeniedException();
                }
            }
        }
    }
}
