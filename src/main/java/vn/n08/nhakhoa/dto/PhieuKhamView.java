package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.PhieuKham;

import java.time.LocalDate;

/** Phiếu khám kèm thông tin bệnh nhân, bác sĩ, tiền sử/dị ứng và số chỉ định CLS còn chờ. */
public record PhieuKhamView(PhieuKham pk, String tenBenhNhan, String soDienThoai, LocalDate ngaySinh, String tenBacSi,
                            String tienSuBenh, String diUng, int soClsCho) {}
