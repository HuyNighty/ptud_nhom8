package com.quanlynhakhoa.inventory.lohang.service;

import com.quanlynhakhoa.inventory.lohang.dao.LoHangDao;
import com.quanlynhakhoa.inventory.lohang.model.LoHang;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.SQLException;
import java.util.List;

/** Service chỉ cho phép tra cứu LoHang theo permission đã được project cấp. */
public class LoHangService {
    private final DesktopSession phien;
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final LoHangDao dao;

    public LoHangService(DesktopSession phien) { this(phien, new LoHangDao()); }

    public LoHangService(DesktopSession phien, LoHangDao dao) {
        if (phien == null) throw new IllegalArgumentException("Cần phiên đăng nhập.");
        if (dao == null) throw new IllegalArgumentException("Cần DAO lô hàng.");
        this.phien = phien;
        this.dao = dao;
    }

    public boolean coQuyen(Permission quyen) { return phanQuyen.can(phien, quyen); }

    public List<LoHang> timKiem(String tuKhoa, String loaiHang, String hanSuDung)
            throws SQLException {
        phanQuyen.require(phien, Permission.INVENTORY_READ);
        String keyword = tuKhoa == null ? "" : tuKhoa.trim();
        if (keyword.length() > 100) throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        String kind = loaiHang == null || loaiHang.isBlank() ? LoHang.LOAI_TAT_CA : loaiHang.trim();
        if (!LoHang.LOAI_TAT_CA.equals(kind) && !LoHang.LOAI_THUOC.equals(kind)
                && !LoHang.LOAI_VAT_TU.equals(kind)) {
            throw new IllegalArgumentException("Bộ lọc loại mặt hàng không hợp lệ.");
        }
        String expiry = hanSuDung == null || hanSuDung.isBlank() ? LoHang.HAN_TAT_CA : hanSuDung.trim();
        if (!LoHang.HAN_TAT_CA.equals(expiry) && !LoHang.HAN_HET_HAN.equals(expiry)
                && !LoHang.HAN_CON_HAN.equals(expiry)) {
            throw new IllegalArgumentException("Bộ lọc hạn sử dụng không hợp lệ.");
        }
        return dao.timKiem(keyword, kind, expiry);
    }
}
