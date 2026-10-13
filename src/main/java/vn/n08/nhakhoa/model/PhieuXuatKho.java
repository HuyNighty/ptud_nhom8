package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** Bảng PhieuXuatKho. */
public record PhieuXuatKho(
        String maPhieuXuat,
        LocalDateTime ngayXuat,
        String boPhanNhan,
        String nguoiNhan,
        String lyDo,
        String trangThai,
        String maNhanVien) {}
