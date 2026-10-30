package com.quanlynhakhoa.benhnhan.hopdong;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.security.DesktopSession;
import java.sql.SQLException;
import java.util.ArrayList;

public class TraCuuBenhNhanService implements TraCuuBenhNhan {
    private final BenhNhanService benhNhanService;

    public TraCuuBenhNhanService(DesktopSession phienDangNhap) {
        this.benhNhanService = new BenhNhanService(phienDangNhap);
    }

    @Override
    public ArrayList<BenhNhanTomTat> timBenhNhan(String tuKhoa) throws SQLException {
        if (tuKhoa == null) {
            tuKhoa = "";
        }
        ArrayList<BenhNhan> danhSachBenhNhan = benhNhanService.timKiem(tuKhoa, "");

        ArrayList<BenhNhanTomTat> danhSachTomTat = new ArrayList<>();
        for (BenhNhan benhNhan : danhSachBenhNhan) {
            danhSachTomTat.add(doiSangTomTat(benhNhan));
        }
        return danhSachTomTat;
    }

    @Override
    public BenhNhanTomTat layTheoMa(String maBenhNhan) throws SQLException {
        if (maBenhNhan == null || maBenhNhan.isBlank()) {
            throw new IllegalArgumentException("Chưa chọn bệnh nhân.");
        }
        BenhNhan benhNhan = benhNhanService.xemChiTiet(maBenhNhan.trim());
        if (benhNhan == null) {
            return null;
        }
        return doiSangTomTat(benhNhan);
    }

    private BenhNhanTomTat doiSangTomTat(BenhNhan benhNhan) {
        return new BenhNhanTomTat(
                benhNhan.getMaBenhNhan(),
                benhNhan.getHoTen(),
                benhNhan.getNgaySinh(),
                benhNhan.getGioiTinh(),
                benhNhan.getSoDienThoai());
    }
}
