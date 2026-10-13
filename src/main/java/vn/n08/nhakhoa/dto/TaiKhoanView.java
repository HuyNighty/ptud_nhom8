package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.TaiKhoan;
import vn.n08.nhakhoa.model.TrangThaiNhanVien;
import vn.n08.nhakhoa.model.VaiTro;

/** Tài khoản kèm thông tin nhân viên — dùng cho màn hình Tài khoản & phân quyền. */
public record TaiKhoanView(TaiKhoan tk, String hoTen, VaiTro chucDanh, TrangThaiNhanVien trangThaiNhanVien) {}
