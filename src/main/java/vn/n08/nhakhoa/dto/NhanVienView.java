package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.TrangThaiTaiKhoan;

/** Nhân viên kèm tài khoản (nếu có) — dùng cho danh sách nhân viên. */
public record NhanVienView(NhanVien nv, String maTaiKhoan, String tenDangNhap, TrangThaiTaiKhoan trangThaiTaiKhoan) {}
