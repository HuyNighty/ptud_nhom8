package com.quanlynhakhoa.benhnhan.service;

import com.quanlynhakhoa.benhnhan.dao.BenhNhanDao;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.SQLException;
import java.util.ArrayList;

// Service: kiểm tra QUYỀN và kiểm tra DỮ LIỆU, rồi mới gọi DAO
public class BenhNhanService {
    private DesktopSession phien;   // người đang đăng nhập
    private AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private BenhNhanDao dao = new BenhNhanDao();

    public BenhNhanService(DesktopSession phien) {
        this.phien = phien;
    }

    public ArrayList<BenhNhan> timKiem(String tuKhoa, String gioiTinh) throws SQLException {
        // 1. Kiểm tra quyền: không có quyền xem bệnh nhân thì báo lỗi
        phanQuyen.require(phien, Permission.PATIENT_READ);

        // 2. Kiểm tra dữ liệu nhập
        tuKhoa = tuKhoa.trim();
        if (tuKhoa.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        // Nếu người dùng gõ số điện thoại có khoảng trắng (0909 123 456) thì bỏ khoảng trắng
        String boKhoangTrang = tuKhoa.replace(" ", "");
        if (!boKhoangTrang.isEmpty() && boKhoangTrang.matches("[0-9]+")) {
            if (boKhoangTrang.length() > 10) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ: gồm 10 chữ số và bắt đầu bằng 0.");
            }
            tuKhoa = boKhoangTrang;
        }

        // 3. Gọi DAO lấy dữ liệu
        if (tuKhoa.isEmpty() && gioiTinh.isEmpty()) {
            return dao.getAllBenhNhan();       // không lọc gì → lấy tất cả
        }
        return dao.timKiem(tuKhoa, gioiTinh);
    }

    public BenhNhan xemChiTiet(String maBenhNhan) throws SQLException {
        phanQuyen.require(phien, Permission.PATIENT_READ);
        return dao.timTheoMa(maBenhNhan);
    }
}
