package vn.n08.nhakhoa.model;

import java.time.LocalDate;

/** Bảng HoSoBenhAn — 1 bệnh nhân có đúng 1 hồ sơ bệnh án. */
public record HoSoBenhAn(
        String maHoSo,
        String maBenhNhan,
        LocalDate ngayLap,
        String tienSuBenh,
        String diUng) {}
