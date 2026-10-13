package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.PhuongThucThanhToan;

import java.math.BigDecimal;

/** Dữ liệu một lần thu: số tiền, phương thức, mã giao dịch (CK/POS) hoặc tiền khách đưa (tiền mặt). */
public record ThanhToanForm(BigDecimal soTien, PhuongThucThanhToan phuongThuc, String maGiaoDich, BigDecimal tienKhachDua) {}
