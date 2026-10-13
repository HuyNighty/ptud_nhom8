package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** Bảng PhieuKham — một lượt khám. */
public record PhieuKham(
        String maPhieuKham,
        String maBenhNhan,
        String maHoSo,
        String maNhanVien,
        String maLichHen,
        LocalDateTime thoiGianTiepNhan,
        String trieuChung,
        String chanDoan,
        String ketLuan,
        TrangThaiPhieuKham trangThai,
        String lyDoHuy) {}
