package vn.n08.nhakhoa.dto;

import java.time.LocalDateTime;

/** Dữ liệu tạo lịch hẹn (UC03) hoặc lịch tái khám (UC14, có maKeHoach). */
public record LichHenForm(String maBenhNhan, String maBacSi, LocalDateTime thoiGianHen, String lyDoHen, String maKeHoach) {}
