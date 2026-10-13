package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.GioiTinh;

import java.time.LocalDate;

/** Thông tin hành chính nhập khi tạo/sửa hồ sơ bệnh nhân. */
public record BenhNhanForm(String hoTen, LocalDate ngaySinh, GioiTinh gioiTinh, String soDienThoai, String diaChi, String nguoiLienHe) {}
