package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.BenhNhan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bệnh nhân trong kết quả tìm kiếm: mã hồ sơ, lần khám gần nhất, công nợ (tính, không lưu). */
public record BenhNhanView(BenhNhan bn, String maHoSo, LocalDateTime lanKhamCuoi, BigDecimal congNo) {}
