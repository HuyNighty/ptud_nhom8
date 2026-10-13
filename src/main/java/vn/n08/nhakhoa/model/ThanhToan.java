package vn.n08.nhakhoa.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bảng ThanhToan — mỗi lần thu có phương thức riêng (thay Q&A #8). */
public record ThanhToan(
        String maThanhToan,
        String maHoaDon,
        LocalDateTime thoiGian,
        BigDecimal soTien,
        PhuongThucThanhToan phuongThuc,
        String maGiaoDich,
        BigDecimal tienKhachDua,
        TrangThaiThanhToan trangThai,
        String maNhanVien,
        String maTaiKhoanHuy,
        LocalDateTime thoiGianHuy,
        String lyDoHuy) {
    /** Tiền thối chỉ áp dụng cho tiền mặt; tính ra, không lưu. */
    public BigDecimal tienThoi() { return phuongThuc == PhuongThucThanhToan.TIEN_MAT && tienKhachDua != null ? tienKhachDua.subtract(soTien) : BigDecimal.ZERO; }
}
