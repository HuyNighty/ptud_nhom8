package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.DanhGiaNhanSu;
import vn.n08.nhakhoa.model.NhanVien;

/** Một dòng trong bảng đánh giá theo kỳ: nhân viên và đánh giá (null nếu chưa đánh giá). */
public record DanhGiaView(NhanVien nv, DanhGiaNhanSu dg, String tenNguoiDanhGia) {}
