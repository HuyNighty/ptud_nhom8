package com.quanlynhakhoa.service.kho.lohang;

import com.quanlynhakhoa.dao.kho.lohang.LoHangDao;
import com.quanlynhakhoa.model.kho.ThongTinLoHang;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
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

    public boolean coQuyen(Quyen quyen) { return phanQuyen.can(phien, quyen); }

    public List<ThongTinLoHang> timKiem(String tuKhoa, String loaiHang, String hanSuDung)
            throws SQLException {
        phanQuyen.require(phien, Quyen.INVENTORY_READ);
        String keyword = tuKhoa == null ? "" : tuKhoa.trim();
        if (keyword.length() > 100) throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        String kind = loaiHang == null || loaiHang.isBlank() ? ThongTinLoHang.LOAI_TAT_CA : loaiHang.trim();
        if (!ThongTinLoHang.LOAI_TAT_CA.equals(kind) && !ThongTinLoHang.LOAI_THUOC.equals(kind)
                && !ThongTinLoHang.LOAI_VAT_TU.equals(kind)) {
            throw new IllegalArgumentException("Bộ lọc loại mặt hàng không hợp lệ.");
        }
        String expiry = hanSuDung == null || hanSuDung.isBlank() ? ThongTinLoHang.HAN_TAT_CA : hanSuDung.trim();
        if (!ThongTinLoHang.HAN_TAT_CA.equals(expiry) && !ThongTinLoHang.HAN_HET_HAN.equals(expiry)
                && !ThongTinLoHang.HAN_CON_HAN.equals(expiry)) {
            throw new IllegalArgumentException("Bộ lọc hạn sử dụng không hợp lệ.");
        }
        return dao.timKiem(keyword, kind, expiry);
    }
}
