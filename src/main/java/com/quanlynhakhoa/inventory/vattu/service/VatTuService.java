package com.quanlynhakhoa.inventory.vattu.service;

import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.inventory.vattu.dao.VatTuDao;
import com.quanlynhakhoa.inventory.vattu.model.VatTu;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** Service kiểm tra quyền/nghiệp vụ và điều phối transaction cho danh mục Vật tư. */
public class VatTuService {
    private final DesktopSession phien;
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final VatTuDao dao;

    public VatTuService(DesktopSession phien) {
        this(phien, new VatTuDao());
    }

    public VatTuService(DesktopSession phien, VatTuDao dao) {
        if (phien == null) throw new IllegalArgumentException("Cần phiên đăng nhập.");
        if (dao == null) throw new IllegalArgumentException("Cần DAO vật tư.");
        this.phien = phien;
        this.dao = dao;
    }

    public boolean coQuyen(Permission quyen) {
        return phanQuyen.can(phien, quyen);
    }

    public List<VatTu> timKiem(String tuKhoa) throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_READ);
        String keyword = tuKhoa == null ? "" : tuKhoa.trim();
        if (keyword.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        return dao.timKiem(keyword);
    }

    public void them(VatTu vatTu) throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_MANAGE);
        kiemTraVaChuanHoa(vatTu);
        try (Connection con = TeamDevConnection.open()) {
            con.setAutoCommit(false);
            try {
                if (dao.tonTai(con, vatTu.getMaVatTu())) {
                    throw new IllegalArgumentException("Mã vật tư đã tồn tại.");
                }
                dao.them(con, vatTu);
                dao.ghiNhatKy(con, phien.accountId(), "Thêm vật tư", vatTu.getMaVatTu());
                con.commit();
            } catch (SQLException | RuntimeException loi) {
                rollback(con, loi);
                if (loi instanceof SQLException sql && (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627)) {
                    throw new IllegalArgumentException("Mã vật tư đã tồn tại.", sql);
                }
                throw loi;
            }
        }
    }

    public void capNhat(VatTu vatTu) throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_MANAGE);
        kiemTraVaChuanHoa(vatTu);
        try (Connection con = TeamDevConnection.open()) {
            con.setAutoCommit(false);
            try {
                if (!dao.capNhat(con, vatTu)) {
                    throw new IllegalArgumentException("Không tìm thấy vật tư " + vatTu.getMaVatTu() + ".");
                }
                dao.ghiNhatKy(con, phien.accountId(), "Cập nhật vật tư", vatTu.getMaVatTu());
                con.commit();
            } catch (SQLException | RuntimeException loi) {
                rollback(con, loi);
                throw loi;
            }
        }
    }

    /** Kiểm tra độ dài và dữ liệu theo dbo.VatTu trong schema V5. */
    public static void kiemTraVaChuanHoa(VatTu vatTu) {
        if (vatTu == null) throw new IllegalArgumentException("Thiếu dữ liệu vật tư.");

        String ma = vatTu.getMaVatTu() == null ? "" : vatTu.getMaVatTu().trim().toUpperCase(Locale.ROOT);
        if (ma.isEmpty() || ma.length() > 10 || !ma.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException("Mã vật tư bắt buộc, tối đa 10 ký tự A-Z, 0-9, _ hoặc -.");
        }
        vatTu.setMaVatTu(ma);

        String ten = vatTu.getTenVatTu() == null ? "" : vatTu.getTenVatTu().trim().replaceAll("\\s+", " ");
        if (ten.isEmpty() || ten.length() > 150) {
            throw new IllegalArgumentException("Tên vật tư bắt buộc và tối đa 150 ký tự.");
        }
        vatTu.setTenVatTu(ten);

        String donVi = vatTu.getDonViTinh() == null ? "" : vatTu.getDonViTinh().trim();
        if (donVi.isEmpty() || donVi.length() > 30) {
            throw new IllegalArgumentException("Đơn vị tính bắt buộc và tối đa 30 ký tự.");
        }
        vatTu.setDonViTinh(donVi);

        if (vatTu.getMoTa() == null || vatTu.getMoTa().trim().isEmpty()) {
            vatTu.setMoTa(null);
        } else {
            String moTa = vatTu.getMoTa().trim();
            if (moTa.length() > 300) throw new IllegalArgumentException("Mô tả tối đa 300 ký tự.");
            vatTu.setMoTa(moTa);
        }
        if (vatTu.getDinhMucToiThieu() < 0) {
            throw new IllegalArgumentException("Định mức tối thiểu không được âm.");
        }
    }

    private static void rollback(Connection con, Throwable loi) {
        try {
            con.rollback();
        } catch (SQLException rollbackError) {
            loi.addSuppressed(rollbackError);
        }
    }
}
