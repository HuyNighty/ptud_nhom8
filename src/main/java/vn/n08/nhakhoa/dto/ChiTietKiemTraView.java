package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.ChiTietKiemTraCapCuu;

/** Kết quả một mục trong một lần kiểm tra. */
public record ChiTietKiemTraView(ChiTietKiemTraCapCuu ct, String tenMuc, String donViTinh, int soLuongToiThieu) {}
