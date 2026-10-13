package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.HoaDon;

import java.math.BigDecimal;

/**
 * Hóa đơn kèm giá trị suy ra: tổng tiền (từ ChiTietHoaDon), đã thanh toán (tổng các lần thu THANH_CONG)
 * và còn nợ. Các giá trị này không lưu trong bảng HoaDon.
 */
public record HoaDonView(HoaDon hd, String tenBenhNhan, String soDienThoai, String tenNguoiLap,
                         BigDecimal tongTien, BigDecimal daThanhToan, boolean coYeuCauHuyCho) {
    public BigDecimal conNo() {
        BigDecimal n = tongTien.subtract(daThanhToan);
        return n.signum() < 0 ? BigDecimal.ZERO : n;
    }
}
