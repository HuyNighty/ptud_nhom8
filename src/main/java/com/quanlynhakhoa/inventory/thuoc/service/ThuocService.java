package com.quanlynhakhoa.inventory.thuoc.service;

import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.inventory.thuoc.dao.ThuocDao;
import com.quanlynhakhoa.inventory.thuoc.model.Thuoc;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** Service là biên kiểm tra quyền/nghiệp vụ và điều phối transaction của feature Thuốc. */
public class ThuocService {
    private final DesktopSession phien;
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final ThuocDao dao;

    public ThuocService(DesktopSession phien) {
        this(phien, new ThuocDao());
    }

    public ThuocService(DesktopSession phien, ThuocDao dao) {
        if (phien == null) throw new IllegalArgumentException("Cần phiên đăng nhập.");
        if (dao == null) throw new IllegalArgumentException("Cần DAO thuốc.");
        this.phien = phien;
        this.dao = dao;
    }

    public boolean coQuyen(Permission quyen) {
        return phanQuyen.can(phien, quyen);
    }

    public List<Thuoc> timKiem(String tuKhoa) throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_READ);
        String keyword = tuKhoa == null ? "" : tuKhoa.trim();
        if (keyword.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        return dao.timKiem(keyword);
    }

    public void them(Thuoc thuoc) throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_MANAGE);
        kiemTraVaChuanHoa(thuoc);
        try (Connection con = TeamDevConnection.open()) {
            con.setAutoCommit(false);
            try {
                if (dao.tonTai(con, thuoc.getMaThuoc())) {
                    throw new IllegalArgumentException("Mã thuốc đã tồn tại.");
                }
                dao.them(con, thuoc);
                dao.ghiNhatKy(con, phien.accountId(), "Thêm thuốc", thuoc.getMaThuoc());
                con.commit();
            } catch (SQLException | RuntimeException loi) {
                rollback(con, loi);
                if (loi instanceof SQLException sql && (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627)) {
                    throw new IllegalArgumentException("Mã thuốc đã tồn tại.", sql);
                }
                throw loi;
            }
        }
    }

    public void capNhat(Thuoc thuoc) throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_MANAGE);
        kiemTraVaChuanHoa(thuoc);
        try (Connection con = TeamDevConnection.open()) {
            con.setAutoCommit(false);
            try {
                if (!dao.capNhat(con, thuoc)) {
                    throw new IllegalArgumentException("Không tìm thấy thuốc " + thuoc.getMaThuoc() + ".");
                }
                dao.ghiNhatKy(con, phien.accountId(), "Cập nhật thuốc", thuoc.getMaThuoc());
                con.commit();
            } catch (SQLException | RuntimeException loi) {
                rollback(con, loi);
                throw loi;
            }
        }
    }

    /** Chuẩn hóa và kiểm tra theo giới hạn cột của dbo.Thuoc. */
    public static void kiemTraVaChuanHoa(Thuoc thuoc) {
        if (thuoc == null) throw new IllegalArgumentException("Thiếu dữ liệu thuốc.");

        String ma = thuoc.getMaThuoc() == null ? "" : thuoc.getMaThuoc().trim().toUpperCase(Locale.ROOT);
        if (ma.isEmpty() || ma.length() > 10 || !ma.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException("Mã thuốc bắt buộc, tối đa 10 ký tự A-Z, 0-9, _ hoặc -.");
        }
        thuoc.setMaThuoc(ma);

        String ten = thuoc.getTenThuoc() == null ? "" : thuoc.getTenThuoc().trim().replaceAll("\\s+", " ");
        if (ten.isEmpty() || ten.length() > 150) {
            throw new IllegalArgumentException("Tên thuốc bắt buộc và tối đa 150 ký tự.");
        }
        thuoc.setTenThuoc(ten);

        String donVi = thuoc.getDonViTinh() == null ? "" : thuoc.getDonViTinh().trim();
        if (donVi.isEmpty() || donVi.length() > 30) {
            throw new IllegalArgumentException("Đơn vị tính bắt buộc và tối đa 30 ký tự.");
        }
        thuoc.setDonViTinh(donVi);

        thuoc.setHamLuong(chuanHoaOptional(thuoc.getHamLuong(), 50, "Hàm lượng"));
        thuoc.setHoatChat(chuanHoaOptional(thuoc.getHoatChat(), 150, "Hoạt chất"));
        if (thuoc.getDinhMucToiThieu() < 0) {
            throw new IllegalArgumentException("Định mức tối thiểu không được âm.");
        }
    }

    private static String chuanHoaOptional(String giaTri, int max, String tenTruong) {
        if (giaTri == null || giaTri.trim().isEmpty()) return null;
        String value = giaTri.trim();
        if (value.length() > max) {
            throw new IllegalArgumentException(tenTruong + " tối đa " + max + " ký tự.");
        }
        return value;
    }

    private static void rollback(Connection con, Throwable loi) {
        try {
            con.rollback();
        } catch (SQLException rollbackError) {
            loi.addSuppressed(rollbackError);
        }
    }
}
