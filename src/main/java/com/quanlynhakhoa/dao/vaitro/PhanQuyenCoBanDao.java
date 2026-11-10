package com.quanlynhakhoa.dao.vaitro;

import com.quanlynhakhoa.model.vaitro.DongVaiTro;
import com.quanlynhakhoa.model.vaitro.DongQuyen;
import com.quanlynhakhoa.enums.Quyen;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

/** Truy cập danh mục bằng transaction của Service; không quyết định quyền người dùng. */
public final class PhanQuyenCoBanDao {
    private static final Pattern PERMISSION_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private static PreparedStatement statement(Connection c, String sql) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setQueryTimeout(10);
        return ps;
    }

    public long epoch(Connection c) throws SQLException {
        try (PreparedStatement ps = statement(c, "SELECT epoch FROM dbo.RbacEpoch WHERE singletonId=1");
             ResultSet rows = ps.executeQuery()) {
            if (!rows.next()) throw new SQLException("RBAC V9 chưa sẵn sàng: thiếu epoch.");
            long value = rows.getLong(1);
            if (rows.wasNull() || value < 1 || rows.next()) throw new SQLException("RBAC epoch không hợp lệ.");
            return value;
        }
    }

    private static void exactlyOne(int n) throws SQLException {
        if (n != 1) throw new SQLException("Không thể ghi danh mục RBAC hợp lệ.");
    }

    public List<DongVaiTro> listRoles(Connection c)throws SQLException {
            List<DongVaiTro> result = new ArrayList<>();
            try (PreparedStatement ps = statement(c,
                    "SELECT roleCode,roleName,isSystem,enabled FROM dbo.VaiTro ORDER BY roleCode");
                 ResultSet rows = ps.executeQuery()) {
                while (rows.next()) result.add(new DongVaiTro(rows.getString(1), rows.getNString(2),
                        rows.getBoolean(3), rows.getBoolean(4)));
            }
        return List.copyOf(result);
    }


    public List<DongQuyen> listPermissions(Connection c)throws SQLException {
            List<DongQuyen> result = new ArrayList<>();
            try (PreparedStatement ps = statement(c,
                    "SELECT permissionCode,permissionName FROM dbo.Quyen ORDER BY permissionCode");
                 ResultSet rows = ps.executeQuery()) {
                while (rows.next()) {
                    String code = rows.getString(1);
                    if (code == null || !PERMISSION_CODE.matcher(code).matches())
                        throw new SQLException("RBAC permission code không hợp lệ.");
                    boolean builtIn = false;
                    for (Quyen p : Quyen.values()) if (p.name().equals(code)) builtIn = true;
                    result.add(new DongQuyen(code, rows.getNString(2), builtIn));
                }
            }
        return List.copyOf(result);
    }


    public void taoVaiTro(Connection c,String code,String cleanName,String cleanDescription)throws SQLException {
            try (PreparedStatement ps = statement(c,
                    "INSERT INTO dbo.VaiTro(roleCode,roleName,description,enabled,isSystem,isProtected) " +
                            "VALUES(?,?,?,1,0,0)")) {
                ps.setString(1, code);
                ps.setNString(2, cleanName);
                ps.setNString(3, cleanDescription);
                exactlyOne(ps.executeUpdate());
            }
    }


    public void ganQuyenBanDau(Connection c,String code,Set<Quyen> selected)throws SQLException {
            try (PreparedStatement grant = statement(c,
                    "INSERT INTO dbo.VaiTroQuyen(roleCode,permissionCode) " +
                            "SELECT ?,permissionCode FROM dbo.Quyen WHERE permissionCode=?")) {
                for (Quyen permission : selected) {
                    grant.setString(1, code);
                    grant.setString(2, permission.name());
                    exactlyOne(grant.executeUpdate());
                }
            }
    }


    public void khaiBaoQuyen(Connection c,String code,String cleanName)throws SQLException {
            try (PreparedStatement ps = statement(c,
                    "INSERT INTO dbo.Quyen(permissionCode,permissionName) VALUES(?,?)")) {
                ps.setString(1, code);
                ps.setNString(2, cleanName);
                exactlyOne(ps.executeUpdate());
            }
    }

}
