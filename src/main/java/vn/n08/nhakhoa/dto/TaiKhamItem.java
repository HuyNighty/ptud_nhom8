package vn.n08.nhakhoa.dto;

import java.time.LocalDateTime;

/** Hạng mục đến hạn / quá hạn tái khám, kèm lịch hẹn đang mở (nếu đã hẹn). */
public record TaiKhamItem(ChiTietDieuTriView hangMuc, String maKeHoach, String mucTieu, String maBenhNhan, String tenBenhNhan,
                          String soDienThoai, String maBacSi, String tenBacSi, String maLichHenMo, LocalDateTime thoiGianHen) {}
