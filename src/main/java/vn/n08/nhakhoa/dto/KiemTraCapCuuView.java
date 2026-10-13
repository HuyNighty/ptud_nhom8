package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.KiemTraCapCuu;

/** Một lần kiểm tra hộp cấp cứu kèm người kiểm tra và số mục đạt. */
public record KiemTraCapCuuView(KiemTraCapCuu kt, String tenNguoiKiemTra, int soMuc, int soDat) {}
