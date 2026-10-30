package com.quanlynhakhoa.benhnhan.service;

import com.quanlynhakhoa.benhnhan.dao.BenhNhanDao;
import com.quanlynhakhoa.benhnhan.dao.HoSoBenhAnDao;
import com.quanlynhakhoa.benhnhan.model.HoSoBenhAn;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.SQLException;

// Service HỒ SƠ BỆNH ÁN (UC009 trong báo cáo docs/tuan_cuoi): CHỈ XEM.
//   Hồ sơ bệnh án = lịch sử các lần bệnh nhân đã khám (chẩn đoán, kết luận...).
//   Quyền: MEDICAL_RECORD_READ (bác sĩ; admin có mọi quyền). Lễ tân không mở được.
//   Mỗi lần mở hồ sơ đều GHI NHẬT KÝ người xem (UC009 bước 2).
public class HoSoBenhAnService {
    private final DesktopSession phienDangNhap;   // người đang đăng nhập
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final HoSoBenhAnDao hoSoBenhAnDao;

    public HoSoBenhAnService(DesktopSession phienDangNhap) {
        this(phienDangNhap, TeamDevConnection::open);
    }

    public HoSoBenhAnService(DesktopSession phienDangNhap, BenhNhanDao.ConnectionProvider cachMoKetNoi) {
        this.phienDangNhap = java.util.Objects.requireNonNull(phienDangNhap, "phienDangNhap");
        this.hoSoBenhAnDao = new HoSoBenhAnDao(java.util.Objects.requireNonNull(cachMoKetNoi, "cachMoKetNoi"));
    }

    // Người đang đăng nhập có quyền này không
    public boolean coQuyen(Permission quyen) {
        return phanQuyen.can(phienDangNhap, quyen);
    }

    // Mở hồ sơ bệnh án của 1 bệnh nhân:
    //   1. kiểm tra quyền  →  2. ghi nhật ký người xem  →  3. đọc hồ sơ + các lần khám
    // Bệnh nhân chưa có hồ sơ thì trả về hồ sơ trống (maHoSo = null, không có lần khám).
    public HoSoBenhAn moHoSo(String maBenhNhan) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.MEDICAL_RECORD_READ);
        if (maBenhNhan == null || maBenhNhan.isBlank()) {
            throw new IllegalArgumentException("Chưa chọn bệnh nhân.");
        }

        // Ghi nhật ký TRƯỚC: không ghi được nhật ký thì không hiện hồ sơ
        hoSoBenhAnDao.ghiNhatKyXem(phienDangNhap.accountId(), maBenhNhan);

        HoSoBenhAn hoSoBenhAn = hoSoBenhAnDao.timTheoBenhNhan(maBenhNhan);
        if (hoSoBenhAn == null) {
            hoSoBenhAn = new HoSoBenhAn(null, maBenhNhan, null, null, null);
        }
        hoSoBenhAn.setDanhSachLuotKham(hoSoBenhAnDao.layDanhSachLuotKham(maBenhNhan));
        return hoSoBenhAn;
    }
}
