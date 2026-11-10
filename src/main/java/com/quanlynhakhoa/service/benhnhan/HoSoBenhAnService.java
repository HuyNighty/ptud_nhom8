package com.quanlynhakhoa.service.benhnhan;

import com.quanlynhakhoa.dao.benhnhan.BenhNhanDao;
import com.quanlynhakhoa.dao.benhnhan.HoSoBenhAnDao;
import com.quanlynhakhoa.entity.HoSoBenhAn;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.SQLException;

public class HoSoBenhAnService {
    private final DesktopSession phienDangNhap;
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final HoSoBenhAnDao hoSoBenhAnDao;

    public HoSoBenhAnService(DesktopSession phienDangNhap) {
        this(phienDangNhap, TeamDevConnection::open);
    }

    public HoSoBenhAnService(DesktopSession phienDangNhap, BenhNhanDao.ConnectionProvider cachMoKetNoi) {
        this.phienDangNhap = java.util.Objects.requireNonNull(phienDangNhap, "phienDangNhap");
        this.hoSoBenhAnDao = new HoSoBenhAnDao(java.util.Objects.requireNonNull(cachMoKetNoi, "cachMoKetNoi"));
    }

    public boolean coQuyen(Quyen quyen) {
        return phanQuyen.can(phienDangNhap, quyen);
    }

    public HoSoBenhAn moHoSo(String maBenhNhan) throws SQLException {
        phanQuyen.require(phienDangNhap, Quyen.MEDICAL_RECORD_READ);
        if (maBenhNhan == null || maBenhNhan.isBlank()) {
            throw new IllegalArgumentException("Chưa chọn bệnh nhân.");
        }

        hoSoBenhAnDao.ghiNhatKyXem(phienDangNhap.accountId(), maBenhNhan);

        HoSoBenhAn hoSoBenhAn = hoSoBenhAnDao.timTheoBenhNhan(maBenhNhan);
        if (hoSoBenhAn == null) {
            hoSoBenhAn = new HoSoBenhAn(null, maBenhNhan, null, null, null);
        }
        hoSoBenhAn.setDanhSachLuotKham(hoSoBenhAnDao.layDanhSachLuotKham(maBenhNhan));
        return hoSoBenhAn;
    }
}
