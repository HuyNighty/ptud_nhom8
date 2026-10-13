package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.ChiTietHoaDon;

/** Dòng hóa đơn kèm tên mục và loại mục (Dịch vụ / Thuốc / Vật tư). */
public record ChiTietHoaDonView(ChiTietHoaDon ct, String tenMuc, String loaiMuc) {}
