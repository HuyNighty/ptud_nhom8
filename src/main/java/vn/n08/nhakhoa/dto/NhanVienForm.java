package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.GioiTinh;
import vn.n08.nhakhoa.model.VaiTro;

import java.time.LocalDate;

/** Dữ liệu nhập khi thêm/sửa nhân viên (chưa kiểm tra). */
public record NhanVienForm(String hoTen, String soDienThoai, VaiTro chucDanh, LocalDate ngaySinh, GioiTinh gioiTinh,
                           String email, LocalDate ngayVaoLam, String chuyenMon) {}
