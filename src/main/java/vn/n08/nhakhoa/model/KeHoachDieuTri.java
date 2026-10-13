package vn.n08.nhakhoa.model;

import java.time.LocalDate;

/** Bảng KeHoachDieuTri. */
public record KeHoachDieuTri(
        String maKeHoach,
        String maHoSo,
        String maPhieuKham,
        LocalDate ngayLap,
        String mucTieu,
        TrangThaiKeHoach trangThai,
        String maNhanVien,
        String lyDoHuy) {}
