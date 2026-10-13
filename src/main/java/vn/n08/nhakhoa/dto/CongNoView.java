package vn.n08.nhakhoa.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Công nợ theo bệnh nhân (UC05) — tính từ hóa đơn chưa hủy còn nợ. */
public record CongNoView(String maBenhNhan, String tenBenhNhan, String soDienThoai, int soHoaDon, BigDecimal tongNo, LocalDateTime hoaDonCuNhat) {}
