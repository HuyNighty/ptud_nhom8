package com.quanlynhakhoa.nghiepvu.benhnhan.service;

import com.quanlynhakhoa.nghiepvu.benhnhan.dao.BenhNhanDao;
import com.quanlynhakhoa.nghiepvu.benhnhan.dao.HoSoBenhAnDao;
import com.quanlynhakhoa.nghiepvu.benhnhan.model.HoSoBenhAn;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
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

    public boolean coQuyen(Permission quyen) {
        return phanQuyen.can(phienDangNhap, quyen);
    }

    public HoSoBenhAn moHoSo(String maBenhNhan) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.MEDICAL_RECORD_READ);
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
