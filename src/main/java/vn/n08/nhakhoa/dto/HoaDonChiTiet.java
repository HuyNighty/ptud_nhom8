package vn.n08.nhakhoa.dto;

import java.util.List;

/** Chi tiết hóa đơn: dòng, lịch sử các lần thu, yêu cầu hủy. */
public record HoaDonChiTiet(HoaDonView hoaDon, List<ChiTietHoaDonView> dong, List<ThanhToanView> thanhToan, List<YeuCauHuyView> yeuCauHuy) {}
